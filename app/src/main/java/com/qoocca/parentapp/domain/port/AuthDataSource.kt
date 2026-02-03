package com.qoocca.parentapp.domain.port

import com.qoocca.parentapp.data.model.LoginResponse
import com.qoocca.parentapp.domain.result.AppResult

interface AuthDataSource {
    fun login(phone: String, onResult: (AppResult<LoginResponse>) -> Unit)
}
