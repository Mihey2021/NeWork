package ru.netology.nework.models.user

data class UsersSelected(
    val users: MutableMap<Long, String> = mutableMapOf()
): java.io.Serializable
