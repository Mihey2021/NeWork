package ru.netology.nework.models

data class Attachment(
    val url: String = "",
    val type: AttachmentType? = null,
): java.io.Serializable

enum class AttachmentType{
    IMAGE,
    VIDEO,
    AUDIO
}