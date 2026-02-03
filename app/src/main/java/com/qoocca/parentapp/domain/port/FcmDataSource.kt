package com.qoocca.parentapp.domain.port

import com.qoocca.parentapp.data.network.ApiResult

interface FcmDataSource {
    fun registerToken(parentId: Long, fcmToken: String, onResult: (ApiResult<Unit>) -> Unit)
}
