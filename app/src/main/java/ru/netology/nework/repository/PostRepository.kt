package ru.netology.nework.repository

import ru.netology.nework.models.*
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest
import ru.netology.nework.models.user.User

interface PostRepository {
    suspend fun likeById(id: Long, likedByMe: Boolean): Post
    suspend fun getUserById(id: Long): User
    suspend fun save(post: PostCreateRequest)
    suspend fun saveWithAttachment(post: PostCreateRequest, upload: MediaUpload)
    suspend fun upload(upload: MediaUpload): Media
    suspend fun removeById(id: Long)
}