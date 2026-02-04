package com.qoocca.parentapp.data.network

import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val body: String?) : ApiResult<Nothing>()
    data class NetworkError(val exception: IOException) : ApiResult<Nothing>()
    data class UnknownError(val exception: Throwable) : ApiResult<Nothing>()
}
