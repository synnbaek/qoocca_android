package com.qoocca.parentapp.presentation.common

import com.qoocca.parentapp.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class UiMessageFactoryTest {

    @Test
    fun loginFailure_serverError_returnsLoginFailed() {
        val message = UiMessageFactory.loginFailure(AppError.Server(401))
        assertEquals(UiMessageFactory.LOGIN_FAILED, message)
    }

    @Test
    fun loginFailure_networkError_returnsServerConnectionFailed() {
        val message = UiMessageFactory.loginFailure(AppError.Network)
        assertEquals(UiMessageFactory.SERVER_CONNECTION_FAILED, message)
    }

    @Test
    fun receiptListFailure_forbidden_returnsAuthFailed() {
        val message = UiMessageFactory.receiptListFailure(AppError.Forbidden)
        assertEquals(UiMessageFactory.AUTH_FAILED, message)
    }

    @Test
    fun receiptListFailure_server500_returnsCodeMessage() {
        val message = UiMessageFactory.receiptListFailure(AppError.Server(500))
        assertEquals("결제 목록을 가져오지 못했습니다. (코드: 500)", message)
    }

    @Test
    fun paymentFailedCount_formatsMessage() {
        assertEquals(
            "3건의 결제에 실패했습니다. 다시 시도해주세요.",
            UiMessageFactory.paymentFailedCount(3)
        )
    }
}
