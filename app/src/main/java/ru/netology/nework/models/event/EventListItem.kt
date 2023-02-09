package ru.netology.nework.models.event

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.user.UserPreview

data class EventListItem(val event: Event) : java.io.Serializable, DataItem {
    override val id: Long get() = event.id
    override val authorId: Long get() = event.authorId
    override val author: String get() = event.author
    override val authorAvatar: String? get() = event.authorAvatar
    override val authorJob: String? get() = event.authorJob
    override val content: String get() = event.content
    override val datetime: String get() = event.datetime
    override val published: String get() = event.published
    override val coords: Coordinates? get() = event.coords
    override val type: EventType get() = event.type
    override val likeOwnerIds: List<Long> get() = event.likeOwnerIds
    override val likedByMe: Boolean get() = event.likedByMe
    override val speakerIds: List<Long> get() = event.speakerIds
    override val participantsIds: List<Long> get() = event.participantsIds
    override val participatedByMe: Boolean get() = event.participatedByMe
    override val attachment: Attachment? get() = event.attachment
    override val link: String? get() = event.link
    override val ownedByMe: Boolean get() = event.ownedByMe
    override val users: Map<Long, UserPreview> get() = event.users
    override val mentionIds: List<Long> get() = listOf()
    override val mentionedMe: Boolean get() = false
    override val isPlayed: Boolean get() = event.isPlayed
    override val initInAudioPlayer: Boolean get() = event.initInAudioPlayer
}
