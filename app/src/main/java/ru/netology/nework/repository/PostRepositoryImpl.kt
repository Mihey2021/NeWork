package ru.netology.nework.repository

import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ApiError
import ru.netology.nework.errors.AppError
import ru.netology.nework.errors.NetworkError
import ru.netology.nework.errors.UnknownError
import ru.netology.nework.models.Attachment
import ru.netology.nework.models.AttachmentType
import ru.netology.nework.models.Media
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest
import java.io.IOException
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : PostRepository {

    override suspend fun likeById(id: Long, likedByMe: Boolean): Post {
        if (likedByMe) {
            return disLikeById(id)
        }

        try {
            val response = apiService.likeById(id)
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

    private suspend fun disLikeById(id: Long): Post {
        try {
            val response = apiService.dislikeById(id)
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

    override suspend fun save(post: PostCreateRequest): Post {
        try {
            val response = apiService.save(post)
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

    override suspend fun saveWithAttachment(post: PostCreateRequest, upload: MediaUpload): Post {
        try {
            val media = upload(upload)
            val postWithAttachment =
                post.copy(attachment = Attachment(media.url, AttachmentType.IMAGE))
            return save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: ApiError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
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

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeById(id)
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

}