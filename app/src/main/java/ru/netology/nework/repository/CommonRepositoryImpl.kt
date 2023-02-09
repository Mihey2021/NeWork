package ru.netology.nework.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.R
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.user.User
import java.io.IOException
import javax.inject.Inject

private val gson = Gson()

class CommonRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext
    private val context: Context,
) : CommonRepository {

    override suspend fun getUserById(id: Long): User {
        try {
            val response = apiService.getUserById(id)
            if (!response.isSuccessful) {
                val errJson = response.errorBody()?.string()
                if (errJson.isNullOrBlank()) throw UnknownError
                throw getErrorResponse(errJson)
            }
            return response.body() ?: throw getErrorResponse()
        } catch (e: ErrorResponse) {
            throw e
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
                val errJson = response.errorBody()?.string()
                if (errJson.isNullOrBlank()) throw UnknownError
                throw getErrorResponse(errJson)
            }
            return response.body() ?: throw getErrorResponse()
        } catch (e: ErrorResponse) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
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