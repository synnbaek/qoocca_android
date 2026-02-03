package com.qoocca.parentapp.presentation.common

import com.qoocca.parentapp.data.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class UiMessageFactoryTest {

    @Test
    fun loginFailure_httpError_returnsLoginFailed() {
        val message = UiMessageFactory.loginFailure(ApiResult.HttpError(401, "bad"))
        assertEquals(UiMessageFactory.LOGIN_FAILED, message)
    }

    @Test
    fun loginFailure_networkError_returnsServerConnectionFailed() {
        val message = UiMessageFactory.loginFailure(ApiResult.NetworkError(IOException("boom")))
        assertEquals(UiMessageFactory.SERVER_CONNECTION_FAILED, message)
    }

    @Test
    fun receiptListFailure_403_returnsAuthFailed() {
        val message = UiMessageFactory.receiptListFailure(ApiResult.HttpError(403, "forbidden"))
        assertEquals(UiMessageFactory.AUTH_FAILED, message)
    }

    @Test
    fun receiptListFailure_500_returnsCodeMessage() {
        val message = UiMessageFactory.receiptListFailure(ApiResult.HttpError(500, "err"))
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
