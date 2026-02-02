package com.qoocca.parentapp

import android.content.Context

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveAuthData(parentId: Long, token: String) {
        prefs.edit()
            .putLong("parentId", parentId)
            .putString("accessToken", token)
            .apply()
    }

    fun getParentId(): Long {
        return prefs.getLong("parentId", -1L)
    }

    fun getToken(): String? {
        return prefs.getString("accessToken", null)
    }

    fun isLoggedIn(): Boolean {
        // Check for both parentId and token
        return getParentId() != -1L && getToken() != null
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
