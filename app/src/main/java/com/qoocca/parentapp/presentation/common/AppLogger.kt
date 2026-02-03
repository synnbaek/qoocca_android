package com.qoocca.parentapp.presentation.common

import android.util.Log

object AppLogger {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun maskToken(token: String?): String {
        if (token.isNullOrBlank()) return "<empty>"
        val trimmed = token.trim()
        if (trimmed.length <= 8) return "****"
        return "${trimmed.take(4)}...${trimmed.takeLast(4)}"
    }
}
