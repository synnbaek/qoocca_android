package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.data.model.LoginResponse
import com.qoocca.parentapp.domain.port.AuthDataSource
import com.qoocca.parentapp.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    @Test
    fun execute_delegatesToAuthDataSource() {
        var capturedPhone: String? = null
        val fakeDataSource = object : AuthDataSource {
            override fun login(phone: String, onResult: (AppResult<LoginResponse>) -> Unit) {
                capturedPhone = phone
                onResult(AppResult.Success(LoginResponse(1L, "token", "parent")))
            }
        }

        val useCase = LoginUseCase(fakeDataSource)
        var result: AppResult<LoginResponse>? = null

        useCase.execute("01012345678") { result = it }

        assertEquals("01012345678", capturedPhone)
        assertTrue(result is AppResult.Success)
    }
}
