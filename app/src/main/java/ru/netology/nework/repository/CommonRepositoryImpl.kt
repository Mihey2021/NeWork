package ru.netology.nework.repository

import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ApiError
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.user.User
import java.io.IOException
import javax.inject.Inject

class CommonRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
): CommonRepository {

    override suspend fun getUserById(id: Long): User {
        try {
            val response = apiService.getUserById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.errorBody()?.string() ?: response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllUsers(): List<User> {
        try {
            val response = apiService.getAllUsers()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.errorBody()?.string() ?: response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

}