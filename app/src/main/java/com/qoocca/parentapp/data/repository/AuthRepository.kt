package com.qoocca.parentapp.data.repository

import com.qoocca.parentapp.data.model.LoginResponse
import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.network.ApiErrorMapper
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.domain.port.AuthDataSource
import com.qoocca.parentapp.domain.result.AppResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepository(
    private val apiClient: ApiClient
) : AuthDataSource {
    override fun login(phone: String, onResult: (AppResult<LoginResponse>) -> Unit) {
        val requestJson = JSONObject().apply {
            put("parentPhone", phone)
        }

        val body = requestJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = apiClient.buildPost(path = "/api/parent/auth/login", body = body)
        apiClient.executeAsync(request) { result ->
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val json = JSONObject(result.data)
                        onResult(
                            AppResult.Success(
                                LoginResponse(
                                    parentId = json.getLong("parentId"),
                                    accessToken = json.getString("accessToken"),
                                    parentName = json.getString("parentName")
                                )
                            )
                        )
                    } catch (e: Exception) {
                        onResult(AppResult.Failure(ApiErrorMapper.from(ApiResult.UnknownError(e))))
                    }
                }

                else -> onResult(AppResult.Failure(ApiErrorMapper.from(result)))
            }
        }
    }
}
