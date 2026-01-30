package com.qoocca.parentapp

import android.content.Context

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveParentId(parentId: Long) {
        prefs.edit().putLong("parentId", parentId).apply()
    }

    fun getParentId(): Long {
        return prefs.getLong("parentId", -1L)
    }

    fun isLoggedIn(): Boolean = getParentId() != -1L

    fun logout() {
        prefs.edit().clear().apply()
    }
}
