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
            assertTrue(error?.message.orEmpty().contains("No access to this voice_id"))
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
        audioFormat = "wav"
    )
}
