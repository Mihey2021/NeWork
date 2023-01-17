package ru.netology.nework.repository

import ru.netology.nework.models.*

interface PostRepository {
    suspend fun likeById(id: Int, likedByMe: Boolean): Post
    suspend fun getUserById(id: Int): User
    suspend fun save(post: PostCreateRequest)
    suspend fun saveWithAttachment(post: PostCreateRequest, upload: MediaUpload)
    suspend fun upload(upload: MediaUpload): Media
    suspend fun removeById(id: Int)
}