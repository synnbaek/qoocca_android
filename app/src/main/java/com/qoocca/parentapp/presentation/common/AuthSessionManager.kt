package com.qoocca.parentapp.presentation.common

import android.content.Context
import com.qoocca.parentapp.AuthManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AuthSessionEvent {
    data object SessionExpired : AuthSessionEvent
}

object AuthSessionManager {
    private val _events = MutableSharedFlow<AuthSessionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onAuthFailure(context: Context) {
        AuthManager(context).logout()
        _events.tryEmit(AuthSessionEvent.SessionExpired)
    }
}
