package com.qoocca.parentapp.presentation.common

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AppEventLogger {

    fun logEvent(context: Context, name: String, params: Map<String, Any?> = emptyMap()) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
    }

    fun recordNonFatal(throwable: Throwable, attributes: Map<String, String> = emptyMap()) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        attributes.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        crashlytics.recordException(throwable)
    }
}
