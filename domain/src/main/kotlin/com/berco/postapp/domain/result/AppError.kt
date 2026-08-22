package com.berco.postapp.domain.result

sealed interface AppError {
    data object Network : AppError
    data class Server(val code: Int) : AppError
    data object Serialization : AppError
    data class Unknown(val cause: Throwable) : AppError
}
