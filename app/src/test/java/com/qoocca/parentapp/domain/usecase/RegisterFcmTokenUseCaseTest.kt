package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.port.FcmDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterFcmTokenUseCaseTest {

    @Test
    fun execute_delegatesToFcmDataSource() {
        var capturedParentId: Long? = null
        var capturedToken: String? = null
        val fakeDataSource = object : FcmDataSource {
            override fun registerToken(parentId: Long, fcmToken: String, onResult: (ApiResult<Unit>) -> Unit) {
                capturedParentId = parentId
                capturedToken = fcmToken
                onResult(ApiResult.Success(Unit))
            }
        }

        val useCase = RegisterFcmTokenUseCase(fakeDataSource)
        var result: ApiResult<Unit>? = null

        useCase.execute(10L, "fcm-token") { result = it }

        assertEquals(10L, capturedParentId)
        assertEquals("fcm-token", capturedToken)
        assertTrue(result is ApiResult.Success)
    }
}
