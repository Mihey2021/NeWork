package ru.netology.nework.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.R
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.*
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.Token
import java.io.IOException
import javax.inject.Inject

private val gson = Gson()

class AuthAndRegisterRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext
    private val context: Context,
) : AuthAndRegisterRepository {

    override suspend fun authentication(login: String, pass: String): Token {
        try {
            val response = apiService.authentication(login, pass)
            if (!response.isSuccessful) {
                //При неверном логине или пароле сервер возвращает код 400
                if (response.code() == 400) {
                    val errJson = response.errorBody()?.string()
                    if (errJson.isNullOrBlank()) throw UnknownError
                    throw getErrorResponse(errJson)
                } else
                    throw NetworkError
            }

            return response.body() ?: throw getErrorResponse()

        } catch (e: ErrorResponse) {
            throw e
        } catch (e: NetworkError) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registration(login: String, pass: String, name: String): Token {
        try {
            val response = apiService.registration(login, pass, name)
            if (!response.isSuccessful) {
                when (response.code()) {
                    400, 500 -> {
                        val errJson = response.errorBody()?.string()
                        if (errJson.isNullOrBlank()) throw UnknownError
                        throw getErrorResponse(errJson)
                    }
                    else -> {
                        throw NetworkError
                    }
                }
            }

            return response.body() ?: throw getErrorResponse()

        } catch (e: ErrorResponse) {
            throw e
        } catch (e: NetworkError) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registerWithPhoto(
        login: RequestBody,
        pass: RequestBody,
        name: RequestBody,
        avatar: MediaUpload
    ): Token {
        try {
            val media = MultipartBody.Part.createFormData(
                "file",
                avatar.fileDescription.first.name,
                avatar.fileDescription.first.asRequestBody(avatar.fileDescription.third)
            )
            val response = apiService.registerWithPhoto(login, pass, name, media)
            if (!response.isSuccessful) {
                when (response.code()) {
                    400, 500 -> {
                        val errJson = response.errorBody()?.string()
                        if (errJson.isNullOrBlank()) throw UnknownError
                        throw getErrorResponse(errJson)
                    }
                    403 -> {
                        throw RegistrationError //Если пользователь с таким логином существует, сервер возвращает код 403.
                    }
                    else -> {
                        throw NetworkError
                    }
                }
            }

            return response.body() ?: throw getErrorResponse()

        } catch (e: RegistrationError) {
            throw RegistrationError
        } catch (e: ErrorResponse) {
            throw e
        } catch (e: NetworkError) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private fun getErrorResponse(errJson: String? = null): Throwable {
        if (errJson.isNullOrBlank()) return ErrorResponse(context.getString(R.string.error_empty_response))
        return gson.fromJson(
            errJson,
            ErrorResponse::class.java
        )
    }
}