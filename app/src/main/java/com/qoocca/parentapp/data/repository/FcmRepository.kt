package com.qoocca.parentapp.data.repository

import com.qoocca.parentapp.ApiConfig
import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class FcmRepository(
    private val apiClient: ApiClient = ApiClient()
) {
    fun registerToken(
        parentId: Long,
        fcmToken: String,
        onResult: (ApiResult<Unit>) -> Unit
    ) {
        val url = "${ApiConfig.API_BASE_URL}/api/fcm/register"
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("parentId", parentId.toString())
            ?.addQueryParameter("fcmToken", fcmToken)
            ?.build()

        if (url == null) {
            onResult(ApiResult.UnknownError(IllegalStateException("Invalid FCM registration URL")))
            return
        }

        val request = Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody(null))
            .build()

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
