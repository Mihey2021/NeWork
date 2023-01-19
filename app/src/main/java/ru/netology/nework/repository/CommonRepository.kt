package ru.netology.nework.repository

import ru.netology.nework.models.user.User

interface CommonRepository {
    suspend fun getUserById(id: Long): User
    suspend fun getAllUsers(): List<User>
}