package com.qoocca.parentapp.data.network

import com.qoocca.parentapp.domain.error.AppError

object ApiErrorMapper {
    fun from(result: ApiResult<*>): AppError = when (result) {
        is ApiResult.HttpError -> when (result.code) {
            401 -> AppError.Unauthorized
            403 -> AppError.Forbidden
            in 500..599 -> AppError.Server(result.code)
            else -> AppError.Server(result.code)
        }

        is ApiResult.NetworkError -> AppError.Network
        is ApiResult.UnknownError -> AppError.Parsing
        is ApiResult.Success -> AppError.Unknown(null)
    }
}
