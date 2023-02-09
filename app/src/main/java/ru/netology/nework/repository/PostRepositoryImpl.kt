package ru.netology.nework.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.R
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Media
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest
import java.io.IOException
import javax.inject.Inject

private val gson = Gson()

class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext
    private val context: Context,
) : PostRepository {

    override suspend fun likeById(id: Long, likedByMe: Boolean): Post {
        if (likedByMe) {
            return disLikeById(id)
        }

        try {
            val response = apiService.likeById(id)
            if (!response.isSuccessful) {
                when (response.code()) {
                    400, 401, 404 -> {
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

    private suspend fun disLikeById(id: Long): Post {
        try {
            val response = apiService.dislikeById(id)
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

    override suspend fun save(post: PostCreateRequest): Post {
        try {
            val response = apiService.save(post)
            if (!response.isSuccessful) {
                when (response.code()) {
                    400, 401, 403 -> {
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

    override suspend fun saveWithAttachment(post: PostCreateRequest, upload: MediaUpload): Post {
        try {
            val media = upload(upload)
            val postWithAttachment =
                post.copy(attachment = Attachment(media.url, upload.fileDescription.second))
            return save(postWithAttachment)
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

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file",
                upload.fileDescription.first.name,
                upload.fileDescription.first.asRequestBody(upload.fileDescription.third)
            )

            val response = apiService.media(media)
            if (!response.isSuccessful) {
                when (response.code()) {
                    400, 401, 500 -> {
                        val errJson = response.errorBody()?.string()
                        if (errJson.isNullOrBlank()) throw UnknownError
                        throw getErrorResponse(errJson)
                    }
                    else -> {
                        throw NetworkError
                    }
                }
            }

            return response.body()
                ?: throw getErrorResponse()

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

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeById(id)
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

    private fun getErrorResponse(errJson: String? = null): Throwable {
        if (errJson.isNullOrBlank()) return ErrorResponse(context.getString(R.string.error_empty_response))
        return gson.fromJson(
            errJson,
            ErrorResponse::class.java
        )
    }
}
