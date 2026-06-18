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
    val baseUrl: String = "",
    val endpointPath: String = "",
    val model: String = "",
    val defaultVoiceId: String = "",
    val defaultSpeed: Float = 1.0f,
    val defaultEmotion: String? = null,
    val maxCharsPerRequest: Int = 2_000,
    val voices: List<VoiceProfile> = emptyList(),
    val defaultVoiceProfileId: String? = null,
    val useFakeProvider: Boolean = true,
    val directorBaseUrl: String = "",
    val directorEndpointPath: String = "/v1/chat/completions",
    val directorModel: String = ""
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
                baseUrl = prefs[Keys.baseUrl].orEmpty(),
                endpointPath = prefs[Keys.endpointPath].orEmpty(),
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
                useFakeProvider = prefs[Keys.useFakeProvider] ?: true,
                directorBaseUrl = prefs[Keys.directorBaseUrl].orEmpty(),
                directorEndpointPath = prefs[Keys.directorEndpointPath] ?: "/v1/chat/completions",
                directorModel = prefs[Keys.directorModel].orEmpty()
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

    suspend fun deleteVoice(id: String) {
        dataStore.edit { prefs ->
            val voices = VoiceLibrary.deserializeVoices(prefs[Keys.voices].orEmpty()).filterNot { it.id == id }
            prefs[Keys.voices] = VoiceLibrary.serializeVoices(voices)
            if (prefs[Keys.defaultVoiceProfileId] == id) {
                prefs.remove(Keys.defaultVoiceProfileId)
            }
        }
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
    }
}
