package com.qoocca.parentapp.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiResult

class ReceiptRepository(
    private val apiClient: ApiClient = ApiClient(),
    private val gson: Gson = Gson()
) {
    fun fetchReceiptRequests(
        token: String,
        onResult: (ApiResult<List<ParentReceiptResponse>>) -> Unit
    ) {
        val request = apiClient.buildGet(path = "/api/parent/receipt/requests", token = token)
        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val type = object : TypeToken<List<ParentReceiptResponse>>() {}.type
                        val receipts: List<ParentReceiptResponse> = gson.fromJson(result.data, type)
                        onResult(ApiResult.Success(receipts))
                    } catch (e: Exception) {
                        onResult(ApiResult.UnknownError(e))
                    }
                }

                is ApiResult.HttpError -> onResult(result)
                is ApiResult.NetworkError -> onResult(result)
                is ApiResult.UnknownError -> onResult(result)
            }
        }
    }

    fun fetchReceiptDetail(
        token: String,
        receiptId: Long,
        onResult: (ApiResult<ParentReceiptResponse>) -> Unit
    ) {
        val request = apiClient.buildGet(path = "/api/parent/receipt/$receiptId", token = token)
        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val receipt = gson.fromJson(result.data, ParentReceiptResponse::class.java)
                        onResult(ApiResult.Success(receipt))
                    } catch (e: Exception) {
                        onResult(ApiResult.UnknownError(e))
                    }
                }

                is ApiResult.HttpError -> onResult(result)
                is ApiResult.NetworkError -> onResult(result)
                is ApiResult.UnknownError -> onResult(result)
            }
        }
    }
}
