package ru.netology.nework.errors

import java.io.IOException

sealed class AppError(errorMessage: String) : RuntimeException(errorMessage) {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is RegistrationError -> e
            is ErrorResponse -> e
            is AppError -> e
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

data class ErrorResponse (val reason: String) : AppError(reason)
object RegistrationError : AppError("A user with this username already exists")
object NetworkError : AppError("A network error has occurred")
object UnknownError : AppError("An unknown error has occurred")