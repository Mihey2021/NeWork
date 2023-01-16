package ru.netology.nework.models

data class PostCreated(
    val id: Int,
    val content: String,
    val coords: Coordinates? = null,
    val link: String? = null,
    val attachment: Attachment? = null,
    val mentionIds: List<Int> = emptyList(),
)