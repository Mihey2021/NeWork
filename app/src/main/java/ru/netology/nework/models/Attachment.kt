package ru.netology.nework.models

data class Attachment(
    val url: String = "",
    val type: AttachmentType? = null,
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO
}