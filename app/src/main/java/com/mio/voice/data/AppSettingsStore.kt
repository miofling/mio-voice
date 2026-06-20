package com.mio.voice.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val baseUrl: String = "https://api.minimax.chat",
    val endpointPath: String = "/v1/t2a_v2",
    val model: String = "",
    val defaultVoiceId: String = "",
    val defaultSpeed: Float = 1.0f,
    val defaultEmotion: String? = null,
    val maxCharsPerRequest: Int = 2_000,
    val voices: List<VoiceProfile> = emptyList(),
    val defaultVoiceProfileId: String? = null,
    val useFakeProvider: Boolean = false,
    val directorBaseUrl: String = "",
    val directorEndpointPath: String = "/v1/chat/completions",
    val directorModel: String = "",
    /** AI 导演自定义系统提示词；为空表示使用内置默认提示词。 */
    val directorSystemPrompt: String = "",
    /** AI 导演「自动表演标记」开关：开启后 AI 可在文本中插入语气词/停顿标记（仅 MiniMax speech-2.8 支持）。默认关。 */
    val directorAutoTags: Boolean = false,
    /** AI 导演分段预览草稿（JSON）；为空表示无草稿。用于切 tab/切后台/重建后恢复预览。 */
    val directorDraftJson: String = ""
) {
    fun providerConfig(apiKey: String? = null): ProviderConfig = ProviderConfig(
        baseUrl = baseUrl,
        endpointPath = endpointPath,
        apiKey = apiKey,
        model = model,
        defaultVoiceId = defaultVoiceId,
        defaultSpeed = defaultSpeed,
        defaultEmotion = defaultEmotion,
        audioFormat = "wav",
        maxCharsPerRequest = maxCharsPerRequest
    )

    fun directorConfig(apiKey: String? = null): DirectorConfig = DirectorConfig(
        baseUrl = directorBaseUrl,
        endpointPath = directorEndpointPath.ifBlank { "/v1/chat/completions" },
        apiKey = apiKey,
        model = directorModel
    )
}

class AppSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            AppSettings(
                baseUrl = prefs[Keys.baseUrl]?.takeIf { it.isNotBlank() } ?: "https://api.minimax.chat",
                endpointPath = prefs[Keys.endpointPath]?.takeIf { it.isNotBlank() } ?: "/v1/t2a_v2",
                model = prefs[Keys.model].orEmpty(),
                defaultVoiceId = prefs[Keys.defaultVoiceId].orEmpty(),
                defaultSpeed = prefs[Keys.defaultSpeed] ?: 1.0f,
                defaultEmotion = prefs[Keys.defaultEmotion]?.ifBlank { null },
                maxCharsPerRequest = prefs[Keys.maxCharsPerRequest] ?: 2_000,
                voices = VoiceLibrary.deserializeVoices(
                    value = prefs[Keys.voices].orEmpty(),
                    fallbackEmotion = prefs[Keys.defaultEmotion]?.ifBlank { null },
                    fallbackSpeed = prefs[Keys.defaultSpeed] ?: 1.0f,
                    fallbackPitch = 0
                ),
                defaultVoiceProfileId = prefs[Keys.defaultVoiceProfileId],
                useFakeProvider = prefs[Keys.useFakeProvider] ?: false,
                directorBaseUrl = prefs[Keys.directorBaseUrl].orEmpty(),
                directorEndpointPath = prefs[Keys.directorEndpointPath] ?: "/v1/chat/completions",
                directorModel = prefs[Keys.directorModel].orEmpty(),
                directorSystemPrompt = prefs[Keys.directorSystemPrompt].orEmpty(),
                directorAutoTags = prefs[Keys.directorAutoTags] ?: false,
                directorDraftJson = prefs[Keys.directorDraftJson].orEmpty()
            )
        }

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.baseUrl] = settings.baseUrl
            prefs[Keys.endpointPath] = settings.endpointPath
            prefs[Keys.model] = settings.model
            prefs[Keys.defaultVoiceId] = settings.defaultVoiceId
            prefs[Keys.defaultSpeed] = settings.defaultSpeed
            prefs[Keys.defaultEmotion] = settings.defaultEmotion.orEmpty()
            prefs[Keys.maxCharsPerRequest] = settings.maxCharsPerRequest
            prefs[Keys.voices] = VoiceLibrary.serializeVoices(settings.voices)
            settings.defaultVoiceProfileId?.let { prefs[Keys.defaultVoiceProfileId] = it }
                ?: prefs.remove(Keys.defaultVoiceProfileId)
            prefs[Keys.useFakeProvider] = settings.useFakeProvider
            prefs[Keys.directorBaseUrl] = settings.directorBaseUrl
            prefs[Keys.directorEndpointPath] = settings.directorEndpointPath.ifBlank { "/v1/chat/completions" }
            prefs[Keys.directorModel] = settings.directorModel
            prefs[Keys.directorSystemPrompt] = settings.directorSystemPrompt
            prefs[Keys.directorAutoTags] = settings.directorAutoTags
        }
    }

    suspend fun upsertVoice(profile: VoiceProfile, makeDefault: Boolean) {
        dataStore.edit { prefs ->
            val voices = VoiceLibrary.deserializeVoices(
                value = prefs[Keys.voices].orEmpty(),
                fallbackEmotion = prefs[Keys.defaultEmotion]?.ifBlank { null },
                fallbackSpeed = prefs[Keys.defaultSpeed] ?: 1.0f
            ).toMutableList()
            val normalized = VoiceLibrary.normalizeVoice(
                if (profile.id.isBlank()) profile.copy(id = UUID.randomUUID().toString()) else profile,
                fallbackEmotion = prefs[Keys.defaultEmotion]?.ifBlank { null },
                fallbackSpeed = prefs[Keys.defaultSpeed] ?: 1.0f
            )
            val index = voices.indexOfFirst { it.id == normalized.id }
            if (index >= 0) voices[index] = normalized else voices += normalized
            prefs[Keys.voices] = VoiceLibrary.serializeVoices(voices)
            if (makeDefault) {
                prefs[Keys.defaultVoiceProfileId] = normalized.id
                prefs[Keys.defaultVoiceId] = normalized.voiceId
            }
        }
    }

    /** 批量加入音色：已存在相同 voiceId 的跳过，返回实际新增条数。单事务写回。 */
    suspend fun upsertVoices(profiles: List<VoiceProfile>): Int {
        var added = 0
        dataStore.edit { prefs ->
            val fallbackEmotion = prefs[Keys.defaultEmotion]?.ifBlank { null }
            val fallbackSpeed = prefs[Keys.defaultSpeed] ?: 1.0f
            val voices = VoiceLibrary.deserializeVoices(
                value = prefs[Keys.voices].orEmpty(),
                fallbackEmotion = fallbackEmotion,
                fallbackSpeed = fallbackSpeed
            ).toMutableList()
            profiles.forEach { profile ->
                val normalized = VoiceLibrary.normalizeVoice(
                    if (profile.id.isBlank()) profile.copy(id = UUID.randomUUID().toString()) else profile,
                    fallbackEmotion = fallbackEmotion,
                    fallbackSpeed = fallbackSpeed
                )
                if (!VoiceLibrary.isVoiceIdTaken(voices, normalized.voiceId, excludeProfileId = null)) {
                    voices += normalized
                    added++
                }
            }
            prefs[Keys.voices] = VoiceLibrary.serializeVoices(voices)
        }
        return added
    }

    suspend fun deleteVoice(id: String) {
        dataStore.edit { prefs ->
            val voices = VoiceLibrary.deserializeVoices(prefs[Keys.voices].orEmpty()).filterNot { it.id == id }
            prefs[Keys.voices] = VoiceLibrary.serializeVoices(voices)
            if (prefs[Keys.defaultVoiceProfileId] == id) {
                prefs.remove(Keys.defaultVoiceProfileId)
            }
        }
    }

    /** 单 key 写入 AI 导演分段草稿，不触碰其它设置。 */
    suspend fun saveDirectorDraft(json: String) {
        dataStore.edit { prefs -> prefs[Keys.directorDraftJson] = json }
    }

    /** 清除 AI 导演分段草稿（放弃 / 重新分析失败 / 切非文本模式时调用）。 */
    suspend fun clearDirectorDraft() {
        dataStore.edit { prefs -> prefs.remove(Keys.directorDraftJson) }
    }

    suspend fun clearSettings() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val endpointPath = stringPreferencesKey("endpoint_path")
        val model = stringPreferencesKey("model")
        val defaultVoiceId = stringPreferencesKey("default_voice_id")
        val defaultSpeed = floatPreferencesKey("default_speed")
        val defaultEmotion = stringPreferencesKey("default_emotion")
        val maxCharsPerRequest = intPreferencesKey("max_chars_per_request")
        val voices = stringPreferencesKey("voices")
        val defaultVoiceProfileId = stringPreferencesKey("default_voice_profile_id")
        val useFakeProvider = booleanPreferencesKey("use_fake_provider")
        val directorBaseUrl = stringPreferencesKey("director_base_url")
        val directorEndpointPath = stringPreferencesKey("director_endpoint_path")
        val directorModel = stringPreferencesKey("director_model")
        val directorSystemPrompt = stringPreferencesKey("director_system_prompt")
        val directorAutoTags = booleanPreferencesKey("director_auto_tags")
        val directorDraftJson = stringPreferencesKey("director_draft_json")
    }
}
