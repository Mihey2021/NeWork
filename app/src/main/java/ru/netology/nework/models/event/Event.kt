package ru.netology.nework.models.event

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.user.UserPreview

data class Event(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coordinates? = null,
    val type: EventType,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean,
    val speakerIds: List<Long> = emptyList(),
    val participantsIds: List<Long> = emptyList(),
    val participatedByMe: Boolean,
    val attachment: Attachment? = null,
    val link: String? = null,
    val ownedByMe: Boolean,
    val users: Map<Long, UserPreview>,
): java.io.Serializable

enum class EventType {
    OFFLINE,
    ONLINE
}


