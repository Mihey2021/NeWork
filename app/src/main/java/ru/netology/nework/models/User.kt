package ru.netology.nework.models

data class User(
    val id: Int,
    val login: String,
    val name: String,
    val avatar: String? = null,
): java.io.Serializable
