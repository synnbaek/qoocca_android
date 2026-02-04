package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.port.FcmDataSource

class RegisterFcmTokenUseCase(
    private val fcmDataSource: FcmDataSource
) {
    fun execute(parentId: Long, fcmToken: String, onResult: (ApiResult<Unit>) -> Unit) {
        fcmDataSource.registerToken(parentId, fcmToken, onResult)
    }
}
