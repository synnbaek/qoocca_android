package com.qoocca.parentapp.presentation.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.domain.usecase.LoginUseCase
import com.qoocca.parentapp.domain.usecase.RegisterFcmTokenUseCase

class LoginViewModelFactory(
    private val application: Application,
    private val authManager: AuthManager,
    private val loginUseCase: LoginUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(application, authManager, loginUseCase, registerFcmTokenUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
