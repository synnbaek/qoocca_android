package com.qoocca.parentapp.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiErrorMapper
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.port.ReceiptDataSource
import com.qoocca.parentapp.domain.result.AppResult

class ReceiptRepository(
    private val apiClient: ApiClient,
    private val gson: Gson
) : ReceiptDataSource {
    override fun fetchReceiptRequests(
        token: String,
        onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit
    ) {
        val request = apiClient.buildGet(path = "/api/parent/receipt/requests", token = token)
        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val type = object : TypeToken<List<ParentReceiptResponse>>() {}.type
                        val receipts: List<ParentReceiptResponse> = gson.fromJson(result.data, type)
                        onResult(AppResult.Success(receipts))
                    } catch (e: Exception) {
                        onResult(AppResult.Failure(ApiErrorMapper.from(ApiResult.UnknownError(e))))
                    }
                }

                else -> onResult(AppResult.Failure(ApiErrorMapper.from(result)))
            }
        }
    }

    override fun fetchReceiptDetail(
        token: String,
        receiptId: Long,
        onResult: (AppResult<ParentReceiptResponse>) -> Unit
    ) {
        val request = apiClient.buildGet(path = "/api/parent/receipt/$receiptId", token = token)
        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val receipt = gson.fromJson(result.data, ParentReceiptResponse::class.java)
                        onResult(AppResult.Success(receipt))
                    } catch (e: Exception) {
                        onResult(AppResult.Failure(ApiErrorMapper.from(ApiResult.UnknownError(e))))
                    }
                }

                else -> onResult(AppResult.Failure(ApiErrorMapper.from(result)))
            }
        }
    }
}
