package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.data.model.LoginResponse
import com.qoocca.parentapp.domain.port.AuthDataSource
import com.qoocca.parentapp.domain.result.AppResult

class LoginUseCase(
    private val authDataSource: AuthDataSource
) {
    fun execute(phone: String, onResult: (AppResult<LoginResponse>) -> Unit) {
        authDataSource.login(phone, onResult)
    }
}
