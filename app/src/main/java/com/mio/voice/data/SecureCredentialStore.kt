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

class SecureCredentialStore(context: Context) {
    private val dataStore = context.applicationContext.secureCredentialDataStore

    suspend fun saveApiKey(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        dataStore.edit { prefs ->
            prefs[Keys.cipherText] = encode(encrypted)
            prefs[Keys.iv] = encode(cipher.iv)
            prefs[Keys.version] = 1
        }
    }

    suspend fun readApiKey(): CredentialReadResult {
        val prefs = dataStore.data.first()
        val cipherText = prefs[Keys.cipherText] ?: return CredentialReadResult.Available(null)
        val iv = prefs[Keys.iv] ?: return CredentialReadResult.Available(null)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, decode(iv)))
            val plain = cipher.doFinal(decode(cipherText))
            CredentialReadResult.Available(String(plain, Charsets.UTF_8))
        } catch (error: Exception) {
            CredentialReadResult.DecryptionFailed("API Key 解密失败，请重新填写。")
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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

    private object Keys {
        val cipherText = stringPreferencesKey("cipher_text")
        val iv = stringPreferencesKey("iv")
        val version = intPreferencesKey("version")
    }

    private companion object {
        const val KEY_ALIAS = "mio_voice_api_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
