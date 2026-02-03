package com.qoocca.parentapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val prefs: SharedPreferences = createSecurePrefs()

    init {
        migrateLegacyIfNeeded()
    }

    fun saveAuthData(parentId: Long, token: String) {
        prefs.edit()
            .putLong(KEY_PARENT_ID, parentId)
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getParentId(): Long = prefs.getLong(KEY_PARENT_ID, -1L)

    fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun isLoggedIn(): Boolean {
        return getParentId() != -1L && !getToken().isNullOrBlank()
    }

    fun logout() {
        prefs.edit().clear().apply()
        legacyPrefs.edit().clear().apply()
    }

    private fun createSecurePrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, fallback to legacy prefs", e)
            legacyPrefs
        }
    }

    private fun migrateLegacyIfNeeded() {
        val secureHasData = getParentId() != -1L || !getToken().isNullOrBlank()
        if (secureHasData) return

        val legacyParentId = legacyPrefs.getLong(KEY_PARENT_ID, -1L)
        val legacyToken = legacyPrefs.getString(KEY_ACCESS_TOKEN, null)

        if (legacyParentId != -1L && !legacyToken.isNullOrBlank()) {
            saveAuthData(legacyParentId, legacyToken)
            legacyPrefs.edit().clear().apply()
        }
    }

    companion object {
        private const val TAG = "AuthManager"
        private const val LEGACY_PREFS_NAME = "auth"
        private const val SECURE_PREFS_NAME = "auth_secure"
        private const val KEY_PARENT_ID = "parentId"
        private const val KEY_ACCESS_TOKEN = "accessToken"
    }
}
