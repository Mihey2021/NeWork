package ru.netology.nework.errors

import java.io.IOException

sealed class AppError(errorMessage: String) : RuntimeException(errorMessage) {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is RegistrationError -> e
            is AuthorizationError -> e
            is AppError -> e
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

class ApiError(val status: Int, code: String) : AppError(code)
object RegistrationError: AppError("A user with this username already exists")
object AuthorizationError: AppError("Invalid username or password")
object NetworkError : AppError("error_network")
object UnknownError : AppError("error_unknown")