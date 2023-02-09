package ru.netology.nework.models

import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.user.UserPreview

interface DataItem : java.io.Serializable {
    val id: Long
    val authorId: Long
    val author:	String
    val authorAvatar: String?
    val authorJob: String?
    val content: String
    val published: String
    val coords:	Coordinates?
    val type: EventType
    val link: String?
    val likeOwnerIds: List<Long>
    val mentionIds: List<Long>
    val mentionedMe: Boolean
    val likedByMe: Boolean
    val attachment: Attachment?
    val ownedByMe: Boolean
    val users: Map<Long, UserPreview>
    val datetime: String
    val speakerIds: List<Long>
    val participantsIds: List<Long>
    val participatedByMe: Boolean
    val isPlayed: Boolean
    val initInAudioPlayer: Boolean
}