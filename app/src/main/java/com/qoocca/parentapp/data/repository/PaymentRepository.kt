package com.qoocca.parentapp.data.repository

import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiErrorMapper
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.result.AppResult

class PaymentRepository(
    private val apiClient: ApiClient = ApiClient()
) {
    fun payReceipt(
        token: String,
        receiptId: Long,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        val request = apiClient.buildPost(
            path = "/api/receipt/$receiptId/pay",
            token = token
        )

        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> onResult(AppResult.Success(Unit))
                else -> onResult(AppResult.Failure(ApiErrorMapper.from(result)))
            }
        }
    }
}
