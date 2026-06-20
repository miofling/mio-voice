package com.mio.voice.provider

import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniMaxTtsProviderTest {
    @Test
    fun sendsExpectedHttpRequestAndParsesHexAudio() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "data": {"audio": "52494646", "status": 2},
                      "extra_info": {"audio_format": "wav"},
                      "base_resp": {"status_code": 0, "status_msg": "success"}
                    }
                    """.trimIndent()
                )
            )

            val provider = MiniMaxTtsProvider()
            val result = provider.generate(request(server.url("/").toString()))
            val recorded = server.takeRequest()
            val body = JSONObject(recorded.body.readUtf8())

            assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
            assertEquals("/v1/t2a_v2", recorded.path)
            assertEquals("speech-2.8-hd", body.getString("model"))
            assertEquals("hello", body.getString("text"))
            assertEquals("voice-a", body.getJSONObject("voice_setting").getString("voice_id"))
            assertEquals("happy", body.getJSONObject("voice_setting").getString("emotion"))
            assertEquals(2, body.getJSONObject("voice_setting").getInt("pitch"))
            assertEquals("wav", body.getJSONObject("audio_setting").getString("format"))
            assertEquals("wav", result.audioFormat)
            assertTrue(result.audioBytes.isNotEmpty())
        }
    }

    @Test
    fun mapsBusinessErrorsWithoutLeakingCredential() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "data": null,
                      "base_resp": {"status_code": 2042, "status_msg": "no access"}
                    }
                    """.trimIndent()
                )
            )

            val provider = MiniMaxTtsProvider()
            val error = runCatching { provider.generate(request(server.url("/").toString())) }.exceptionOrNull()

            assertTrue(error is MiniMaxTtsException)
            assertTrue(error?.message.orEmpty().contains("无权使用这个 voice_id"))
            assertTrue(!error?.message.orEmpty().contains("test-key"))
        }
    }

    @Test
    fun fetchesModelsFromOpenAiCompatibleEndpoint() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "object": "list",
                      "data": [
                        {"id": "speech-2.8-hd", "object": "model"},
                        {"id": "speech-2.8-turbo", "object": "model"}
                      ]
                    }
                    """.trimIndent()
                )
            )

            val provider = MiniMaxTtsProvider()
            val models = provider.fetchModels(
                ProviderConfig(
                    baseUrl = server.url("/").toString(),
                    apiKey = "test-key"
                )
            )
            val recorded = server.takeRequest()

            assertEquals("/v1/models", recorded.path)
            assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
            assertEquals(listOf("speech-2.8-hd", "speech-2.8-turbo"), models)
        }
    }

    @Test
    fun fetchesSystemVoicesViaGetVoiceEndpoint() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "system_voice": [
                        {"voice_id": "male-qn-qingse", "voice_name": "青涩青年"},
                        {"voice_id": "female-shaonv", "voice_name": "少女"},
                        {"voice_id": "", "voice_name": "空ID应跳过"}
                      ],
                      "voice_cloning": [
                        {"voice_id": "cloned-1", "voice_name": "克隆音色"}
                      ],
                      "base_resp": {"status_code": 0, "status_msg": "success"}
                    }
                    """.trimIndent()
                )
            )

            val provider = MiniMaxTtsProvider()
            val voices = provider.fetchSystemVoices(
                ProviderConfig(baseUrl = server.url("/").toString(), apiKey = "test-key")
            )
            val recorded = server.takeRequest()
            val body = JSONObject(recorded.body.readUtf8())

            assertEquals("/v1/get_voice", recorded.path)
            assertEquals("POST", recorded.method)
            assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
            assertEquals("all", body.getString("voice_type"))
            // 只取 system_voice，跳过空 voice_id，按 voice_name 排序。
            assertEquals(
                listOf(
                    OfficialVoice("female-shaonv", "少女"),
                    OfficialVoice("male-qn-qingse", "青涩青年")
                ),
                voices
            )
        }
    }

    @Test
    fun systemVoicesBusinessErrorDoesNotLeakCredential() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"base_resp": {"status_code": 1004, "status_msg": "invalid api key"}}"""
                )
            )

            val provider = MiniMaxTtsProvider()
            val error = runCatching {
                provider.fetchSystemVoices(
                    ProviderConfig(baseUrl = server.url("/").toString(), apiKey = "test-key")
                )
            }.exceptionOrNull()

            assertTrue(error is MiniMaxTtsException)
            assertTrue(!error?.message.orEmpty().contains("test-key"))
        }
    }

    private fun request(baseUrl: String) = TtsRequest(
        providerProfileId = "minimax",
        config = ProviderConfig(
            baseUrl = baseUrl,
            endpointPath = "/v1/t2a_v2",
            apiKey = "test-key",
            model = "speech-2.8-hd",
            defaultVoiceId = "voice-a",
            audioFormat = "wav"
        ),
        text = "hello",
        voiceId = "voice-a",
        model = "speech-2.8-hd",
        speed = 1.0f,
        emotion = "happy",
        pitch = 2,
        audioFormat = "wav"
    )
}
