package ru.netology.nework.repository

import ru.netology.nework.models.Media
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest

interface PostRepository {
    suspend fun likeById(id: Long, likedByMe: Boolean): Post
    suspend fun save(post: PostCreateRequest): Post
    suspend fun saveWithAttachment(post: PostCreateRequest, upload: MediaUpload): Post
    suspend fun upload(upload: MediaUpload): Media
    suspend fun removeById(id: Long)
}