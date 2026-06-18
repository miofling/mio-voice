package com.mio.voice.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.secureCredentialDataStore by preferencesDataStore(name = "secure_credentials")

sealed class CredentialReadResult {
    data class Available(val apiKey: String?) : CredentialReadResult()
    data class DecryptionFailed(val message: String) : CredentialReadResult()
}

enum class CredentialSlot(
    val keyAlias: String,
    val cipherTextKey: String,
    val ivKey: String,
    val versionKey: String,
    val displayName: String
) {
    Tts(
        keyAlias = "mio_voice_api_key_v1",
        cipherTextKey = "cipher_text",
        ivKey = "iv",
        versionKey = "version",
        displayName = "TTS API Key"
    ),
    Director(
        keyAlias = "mio_voice_director_api_key_v1",
        cipherTextKey = "director_cipher_text",
        ivKey = "director_iv",
        versionKey = "director_version",
        displayName = "AI 导演 API Key"
    )
}

class SecureCredentialStore(context: Context) {
    private val dataStore = context.applicationContext.secureCredentialDataStore

    suspend fun saveApiKey(apiKey: String, slot: CredentialSlot = CredentialSlot.Tts) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(slot))
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        val keys = Keys(slot)
        dataStore.edit { prefs ->
            prefs[keys.cipherText] = encode(encrypted)
            prefs[keys.iv] = encode(cipher.iv)
            prefs[keys.version] = 1
        }
    }

    suspend fun readApiKey(slot: CredentialSlot = CredentialSlot.Tts): CredentialReadResult {
        val prefs = dataStore.data.first()
        val keys = Keys(slot)
        val cipherText = prefs[keys.cipherText] ?: return CredentialReadResult.Available(null)
        val iv = prefs[keys.iv] ?: return CredentialReadResult.Available(null)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(slot), GCMParameterSpec(GCM_TAG_BITS, decode(iv)))
            val plain = cipher.doFinal(decode(cipherText))
            CredentialReadResult.Available(String(plain, Charsets.UTF_8))
        } catch (error: Exception) {
            CredentialReadResult.DecryptionFailed("${slot.displayName} 解密失败，请重新填写。")
        }
    }

    suspend fun clear(slot: CredentialSlot = CredentialSlot.Tts) {
        val keys = Keys(slot)
        dataStore.edit { prefs ->
            prefs.remove(keys.cipherText)
            prefs.remove(keys.iv)
            prefs.remove(keys.version)
        }
    }

    private fun getOrCreateKey(slot: CredentialSlot): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(slot.keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            slot.keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)

    private class Keys(slot: CredentialSlot) {
        val cipherText = stringPreferencesKey(slot.cipherTextKey)
        val iv = stringPreferencesKey(slot.ivKey)
        val version = intPreferencesKey(slot.versionKey)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
