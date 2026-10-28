package app.trapanista.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class Level(val label: String, val description: String) {
    A1("A1", "Beginner"),
    A2("A2", "Elementary"),
    B1("B1", "Intermediate"),
    B2("B2", "Upper intermediate"),
}

enum class ClaudeModel(val id: String, val label: String, val supportsFallbacks: Boolean) {
    OPUS("claude-opus-5-5", "Claude Opus 5.5", true),
    SONNET("claude-sonnet-5-5", "Claude Sonnet 5.5", true),
    HAIKU("claude-haiku-5-5", "Claude Haiku 5.5", false),
}

/** Gemini Live models for voice mode, reached through Firebase AI Logic. */
enum class GeminiModel(val id: String, val label: String) {
    LIVE_3_8("gemini-3.8-live", "Gemini 3.8 Live"),
    FLASH_LIVE_3_1("gemini-3.1-flash-live-preview", "Gemini 3.1 Flash Live (preview)"),
}

/**
 * Stores settings in SharedPreferences. The API key is encrypted with an AES key
 * that lives in the Android Keystore, so it never sits on disk in plain text.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.let { runCatching { decrypt(it) }.getOrNull() }
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, encrypt(value.trim()))
            }.apply()
        }

    var level: Level
        get() = prefs.getString(KEY_LEVEL, null)?.let { runCatching { Level.valueOf(it) }.getOrNull() } ?: Level.A2
        set(value) = prefs.edit().putString(KEY_LEVEL, value.name).apply()

    var model: ClaudeModel
        get() = prefs.getString(KEY_MODEL, null)?.let { runCatching { ClaudeModel.valueOf(it) }.getOrNull() }
            ?: ClaudeModel.OPUS
        set(value) = prefs.edit().putString(KEY_MODEL, value.name).apply()

    var geminiModel: GeminiModel
        get() = prefs.getString(KEY_GEMINI_MODEL, null)?.let { runCatching { GeminiModel.valueOf(it) }.getOrNull() }
            ?: GeminiModel.LIVE_3_8
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value.name).apply()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        return String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "trapanista_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val KEY_API_KEY = "api_key"
        const val KEY_LEVEL = "level"
        const val KEY_MODEL = "model"
        const val KEY_GEMINI_MODEL = "gemini_model"
    }
}
