package ru.netology.nework.repository

import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ApiError
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.jobs.Job
import java.io.IOException
import javax.inject.Inject

class JobsRepositoryImp @Inject constructor(
    private val apiService: ApiService,
) : JobsRepository {
    override suspend fun getMyJobs(): List<Job> {
        try {
            val response = apiService.getMyJobs()
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
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
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: ApiError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeMyJobById(id: Long) {
        try {
            val response = apiService.removeMyJobById(id)
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            if (response.code() != 200) ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getUserJobs(userId: Long): List<Job> {
        try {
            val response = apiService.getUserJobs(userId)
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}