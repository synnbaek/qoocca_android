package com.qoocca.parentapp.data.network

import com.qoocca.parentapp.ApiConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val emptyBody: RequestBody = ByteArray(0).toRequestBody(null)

    fun buildGet(path: String, token: String? = null): Request {
        return Request.Builder()
            .url(toAbsoluteUrl(path))
            .applyAuthorization(token)
            .get()
            .build()
    }

    fun buildPost(path: String, token: String? = null, body: RequestBody = emptyBody): Request {
        return Request.Builder()
            .url(toAbsoluteUrl(path))
            .applyAuthorization(token)
            .post(body)
            .build()
    }

    fun executeAsync(request: Request, callback: (ApiResult<String>) -> Unit) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(ApiResult.NetworkError(e))
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (it.isSuccessful) {
                        callback(ApiResult.Success(body))
                    } else {
                        callback(ApiResult.HttpError(it.code, body))
                    }
                }
            }
        })
    }

    private fun Request.Builder.applyAuthorization(token: String?): Request.Builder {
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
        return this
    }

    private fun toAbsoluteUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "${ApiConfig.API_BASE_URL}$normalizedPath"
    }
}
