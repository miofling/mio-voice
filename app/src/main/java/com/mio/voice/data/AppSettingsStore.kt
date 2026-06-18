package com.mio.voice.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val voices: List<VoiceProfile> = emptyList(),
    val defaultVoiceProfileId: String? = null,
    val useFakeProvider: Boolean = true
) {
    fun providerConfig(apiKey: String? = null): ProviderConfig = ProviderConfig(
        baseUrl = baseUrl,
        endpointPath = endpointPath,
        apiKey = apiKey,
        model = model,
        defaultVoiceId = defaultVoiceId,
        defaultSpeed = defaultSpeed,
        defaultEmotion = defaultEmotion,
        audioFormat = "wav"
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
                voices = decodeVoices(prefs[Keys.voices].orEmpty()),
                defaultVoiceProfileId = prefs[Keys.defaultVoiceProfileId],
                useFakeProvider = prefs[Keys.useFakeProvider] ?: true
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
            prefs[Keys.voices] = encodeVoices(settings.voices)
            settings.defaultVoiceProfileId?.let { prefs[Keys.defaultVoiceProfileId] = it }
                ?: prefs.remove(Keys.defaultVoiceProfileId)
            prefs[Keys.useFakeProvider] = settings.useFakeProvider
        }
    }

    suspend fun upsertVoice(profile: VoiceProfile, makeDefault: Boolean) {
        dataStore.edit { prefs ->
            val voices = decodeVoices(prefs[Keys.voices].orEmpty()).toMutableList()
            val normalized = if (profile.id.isBlank()) profile.copy(id = UUID.randomUUID().toString()) else profile
            val index = voices.indexOfFirst { it.id == normalized.id }
            if (index >= 0) voices[index] = normalized else voices += normalized
            prefs[Keys.voices] = encodeVoices(voices)
            if (makeDefault) {
                prefs[Keys.defaultVoiceProfileId] = normalized.id
                prefs[Keys.defaultVoiceId] = normalized.voiceId
            }
        }
    }

    suspend fun deleteVoice(id: String) {
        dataStore.edit { prefs ->
            val voices = decodeVoices(prefs[Keys.voices].orEmpty()).filterNot { it.id == id }
            prefs[Keys.voices] = encodeVoices(voices)
            if (prefs[Keys.defaultVoiceProfileId] == id) {
                prefs.remove(Keys.defaultVoiceProfileId)
            }
        }
    }

    suspend fun clearSettings() {
        dataStore.edit { it.clear() }
    }

    private fun encodeVoices(voices: List<VoiceProfile>): String =
        voices.joinToString("\n") { profile ->
            listOf(profile.id, profile.displayName, profile.voiceId).joinToString("\t") { encodePart(it) }
        }

    private fun decodeVoices(value: String): List<VoiceProfile> =
        value.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 3) return@mapNotNull null
                VoiceProfile(
                    id = decodePart(parts[0]),
                    displayName = decodePart(parts[1]),
                    voiceId = decodePart(parts[2])
                )
            }

    private fun encodePart(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decodePart(value: String): String =
        String(Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)

    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val endpointPath = stringPreferencesKey("endpoint_path")
        val model = stringPreferencesKey("model")
        val defaultVoiceId = stringPreferencesKey("default_voice_id")
        val defaultSpeed = floatPreferencesKey("default_speed")
        val defaultEmotion = stringPreferencesKey("default_emotion")
        val voices = stringPreferencesKey("voices")
        val defaultVoiceProfileId = stringPreferencesKey("default_voice_profile_id")
        val useFakeProvider = booleanPreferencesKey("use_fake_provider")
    }
}
