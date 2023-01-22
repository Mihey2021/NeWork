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
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventCreateRequest
import java.io.IOException
import javax.inject.Inject

class EventsRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : EventsRepository {
    override suspend fun likeEventById(id: Long, likedByMe: Boolean): Event {
        if (likedByMe) {
            return disLikeById(id)

        }

        try {
            val response = apiService.likeEventById(id)
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

    private suspend fun disLikeById(id: Long): Event {
        try {
            val response = apiService.dislikeEventById(id)
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

    override suspend fun saveEvent(event: EventCreateRequest): Event {
        try {
            val response = apiService.saveEvent(event)
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

    override suspend fun saveWithAttachment(event: EventCreateRequest, upload: MediaUpload): Event {
        try {
            val media = upload(upload)
            val eventWithAttachment =
                event.copy(attachment = Attachment(media.url, AttachmentType.IMAGE))
            return saveEvent(eventWithAttachment)
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

    override suspend fun removeEventById(id: Long) {
        try {
            val response = apiService.removeEventById(id)
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

    override suspend fun setParticipant(id: Long): Event {
        try {
            val response = apiService.setParticipant(id)
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            if (response.code() != 200) ApiError(response.code(), response.message())
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeParticipant(id: Long): Event {
        try {
            val response = apiService.removeParticipant(id)
            if (!response.isSuccessful) {
                throw ApiError(
                    response.code(),
                    response.errorBody()?.string() ?: response.message()
                )
            }
            if (response.code() != 200) ApiError(response.code(), response.message())
            return response.body() ?: throw ApiError(response.code(), response.message())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}