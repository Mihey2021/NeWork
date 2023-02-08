package ru.netology.nework.models.post

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.user.UserPreview

data class Post (
    val id: Long,
    val authorId: Long,
    val author:	String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val published: String,
    val coords:	Coordinates? = null,
    val link: String? = null,
    val likeOwnerIds: List<Long> = listOf(),
    val mentionIds: List<Long> = listOf(),
    val mentionedMe: Boolean,
    val likedByMe: Boolean,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean,
    val users: Map<Long, UserPreview>,
    val isPlayed: Boolean = false,
    val initInAudioPlayer: Boolean = false,
): java.io.Serializable