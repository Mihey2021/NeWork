package ru.netology.nework.repository

import ru.netology.nework.models.Media
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventCreateRequest

interface EventsRepository {
    suspend fun likeEventById(id: Long, likedByMe: Boolean): Event
    suspend fun saveEvent(event: EventCreateRequest)
    suspend fun saveWithAttachment(event: EventCreateRequest, upload: MediaUpload)
    suspend fun upload(upload: MediaUpload): Media
    suspend fun removeEventById(id: Long)
}