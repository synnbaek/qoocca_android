package com.qoocca.parentapp.data.repository

import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiResult

class PaymentRepository(
    private val apiClient: ApiClient = ApiClient()
) {
    fun payReceipt(
        token: String,
        receiptId: Long,
        onResult: (ApiResult<Unit>) -> Unit
    ) {
        val request = apiClient.buildPost(
            path = "/api/receipt/$receiptId/pay",
            token = token
        )

        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> onResult(ApiResult.Success(Unit))
                is ApiResult.HttpError -> onResult(result)
                is ApiResult.NetworkError -> onResult(result)
                is ApiResult.UnknownError -> onResult(result)
            }
        }
    }
}
