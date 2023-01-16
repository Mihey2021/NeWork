package ru.netology.nework.models

data class Post (
    val id: Int,
    val authorId: Int,
    val author:	String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val published: String,
    val coords:	Coordinates? = null,
    val link: String? = null,
    val likeOwnerIds: List<Int> = emptyList(),
    val mentionIds: List<Int> = emptyList(),
    val mentionedMe: Boolean,
    val likedByMe: Boolean,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean,
    //val users: List<UserPreview> = emptyList(),
): java.io.Serializable