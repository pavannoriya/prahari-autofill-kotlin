package com.prahari.autofill.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage — Prahari JWT token + Gemini API key
 * (React Native ke expo-secure-store ka equivalent)
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "prahari_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---- Prahari JWT ----
    fun savePrahariToken(token: String) {
        prefs.edit()
            .putString(KEY_PRAHARI_TOKEN, token)
            .putLong(KEY_PRAHARI_TOKEN_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getPrahariToken(): String? = prefs.getString(KEY_PRAHARI_TOKEN, null)

    fun isPrahariLoggedIn(): Boolean {
        val token = getPrahariToken() ?: return false
        val time = prefs.getLong(KEY_PRAHARI_TOKEN_TIME, 0)
        // 55 min validity
        return System.currentTimeMillis() - time < TOKEN_VALIDITY_MS
    }

    fun clearPrahariToken() {
        prefs.edit()
            .remove(KEY_PRAHARI_TOKEN)
            .remove(KEY_PRAHARI_TOKEN_TIME)
            .apply()
    }

    // ---- Gemini API key (har auditor apni key dalega) ----
    fun saveGeminiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun getGeminiKey(): String? = prefs.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }

    // ---- Prahari username (auto-fill ke liye, password kabhi save nahi hota) ----
    fun savePrahariUsername(username: String) {
        prefs.edit().putString(KEY_PRAHARI_USER, username).apply()
    }

    fun getPrahariUsername(): String? = prefs.getString(KEY_PRAHARI_USER, null)

    companion object {
        private const val KEY_PRAHARI_TOKEN = "prahari_jwt_token"
        private const val KEY_PRAHARI_TOKEN_TIME = "prahari_token_time"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_PRAHARI_USER = "prahari_username"
        private const val TOKEN_VALIDITY_MS = 55 * 60 * 1000L
    }
}
