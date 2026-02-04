package com.qoocca.parentapp.domain.error

sealed interface AppError {
    data object Unauthorized : AppError
    data object Forbidden : AppError
    data object Network : AppError
    data object Parsing : AppError
    data class Server(val code: Int) : AppError
    data class Unknown(val cause: Throwable?) : AppError
}
