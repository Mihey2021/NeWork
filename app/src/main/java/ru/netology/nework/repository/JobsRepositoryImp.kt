package ru.netology.nework.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.R
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.jobs.Job
import java.io.IOException
import javax.inject.Inject

private val gson = Gson()

class JobsRepositoryImp @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext
    private val context: Context,
) : JobsRepository {
    override suspend fun getMyJobs(): List<Job> {
        try {
            val response = apiService.getMyJobs()
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

    override suspend fun saveMyJob(job: Job): Job {
        try {
            val response = apiService.saveMyJob(job)
            if (!response.isSuccessful) {
                when (response.code()) {
                    401, 403 -> {
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

    override suspend fun removeMyJobById(id: Long) {
        try {
            val response = apiService.removeMyJobById(id)
            if (!response.isSuccessful) {
                when (response.code()) {
                    401, 403 -> {
                        val errJson = response.errorBody()?.string()
                        if (errJson.isNullOrBlank()) throw UnknownError
                        throw getErrorResponse(errJson)
                    }
                    else -> {
                        throw NetworkError
                    }
                }
            }
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

    override suspend fun getUserJobs(userId: Long): List<Job> {
        try {
            val response = apiService.getUserJobs(userId)
            if (!response.isSuccessful) {
                when (response.code()) {
                    401 -> {
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

    private fun getErrorResponse(errJson: String? = null): Throwable {
        if (errJson.isNullOrBlank()) return ErrorResponse(context.getString(R.string.error_empty_response))
        return gson.fromJson(
            errJson,
            ErrorResponse::class.java
        )
    }

}