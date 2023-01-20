package ru.netology.nework.models.user

data class UserDataModel(
    val users: MutableMap<Long, String> = mutableMapOf()
): java.io.Serializable
