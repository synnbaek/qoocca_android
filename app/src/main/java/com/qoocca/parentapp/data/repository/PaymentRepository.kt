package com.qoocca.parentapp.data.repository

import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiErrorMapper
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.port.PaymentDataSource
import com.qoocca.parentapp.domain.result.AppResult

class PaymentRepository(
    private val apiClient: ApiClient
) : PaymentDataSource {

    override fun payReceipt(
        token: String,
        receiptId: Long,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        payReceiptInternal(token = token, receiptId = receiptId, retriesLeft = 1, onResult = onResult)
    }

    private fun payReceiptInternal(
        token: String,
        receiptId: Long,
        retriesLeft: Int,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        val request = apiClient.buildPost(
            path = "/api/receipt/$receiptId/pay",
            token = token
        )

        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> onResult(AppResult.Success(Unit))
                else -> {
                    val mappedError = ApiErrorMapper.from(result)
                    if (mappedError == AppError.Network && retriesLeft > 0) {
                        payReceiptInternal(token, receiptId, retriesLeft - 1, onResult)
                    } else {
                        onResult(AppResult.Failure(mappedError))
                    }
                }
            }
        }
    }
}
