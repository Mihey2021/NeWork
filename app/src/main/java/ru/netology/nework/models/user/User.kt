package ru.netology.nework.models.user

data class User(
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String? = null,
    var isChecked: Boolean = false,
    var itsMe: Boolean = false,
): java.io.Serializable
