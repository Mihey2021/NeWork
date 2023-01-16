package ru.netology.nework.repository

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ApiError
import ru.netology.nework.errors.AuthorizationError
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.RegistrationError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.Token
import java.io.IOException
import javax.inject.Inject

class AuthAndRegisterRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
): AuthAndRegisterRepository {

    override suspend fun authentication(login: String, pass: String): Token {
        try {
            val response = apiService.authentication(login, pass)
            if (!response.isSuccessful) {
                //При неверном логине или пароле сервер возвращает код 400
                if (response.code() == 400)
                    throw AuthorizationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: AuthorizationError) {
            throw AuthorizationError
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registration(login: String, pass: String, name: String): Token {
        try {
            val response = apiService.registration(login, pass, name)
            if (!response.isSuccessful) {
                //TODO: Проверить!!!
                //Если пользователь с таким логином существует, сервер возвращает код 400
                if (response.code() == 400)
                    throw RegistrationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: RegistrationError) {
            throw RegistrationError
        } catch (e: IOException) {
            throw NetworkError
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
                "file", avatar.file.name, avatar.file.asRequestBody()
            )
            val response = apiService.registerWithPhoto(login, pass, name, media)
            if (!response.isSuccessful) {
                //Если пользователь с таким логином существует, сервер возвращает код 403.
                if (response.code() == 403)
                    throw RegistrationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: RegistrationError) {
            throw RegistrationError
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}