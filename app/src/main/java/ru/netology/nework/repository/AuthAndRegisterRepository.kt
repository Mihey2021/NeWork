package ru.netology.nework.repository

import okhttp3.RequestBody
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.Token

interface AuthAndRegisterRepository {

    suspend fun authentication(login: String, pass: String): Token
    suspend fun registration(login: String, pass: String, name: String): Token
    suspend fun registerWithPhoto(login: RequestBody, pass: RequestBody, name: RequestBody, avatar: MediaUpload): Token

}