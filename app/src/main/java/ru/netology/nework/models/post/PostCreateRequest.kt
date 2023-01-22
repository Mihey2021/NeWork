package ru.netology.nework.models.post

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates

data class PostCreateRequest(
    val id: Long,
    val content: String,
    val coords: Coordinates? = null,
    val link: String? = null,
    val attachment: Attachment? = null,
    val mentionIds: List<Long> = listOf(),
)