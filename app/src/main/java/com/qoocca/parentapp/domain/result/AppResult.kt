package com.qoocca.parentapp.domain.result

import com.qoocca.parentapp.domain.error.AppError

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}
