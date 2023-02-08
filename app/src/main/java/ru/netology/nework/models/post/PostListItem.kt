package ru.netology.nework.models.post

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.user.UserPreview

data class PostListItem(val post: Post) : java.io.Serializable, DataItem {
    override val id: Long get() = post.id
    override val authorId: Long get() = post.authorId
    override val author: String get() = post.author
    override val authorAvatar: String? get() = post.authorAvatar
    override val authorJob: String? get() = post.authorJob
    override val content: String get() = post.content
    override val published: String get() = post.published
    override val coords: Coordinates? get() = post.coords
    override val link: String? get() = post.link
    override val likeOwnerIds: List<Long> get() = post.likeOwnerIds
    override val mentionIds: List<Long> get() = post.mentionIds
    override val mentionedMe: Boolean get() = post.mentionedMe
    override val likedByMe: Boolean get() = post.likedByMe
    override val attachment: Attachment? get() = post.attachment
    override val ownedByMe: Boolean get() = post.ownedByMe
    override val users: Map<Long, UserPreview> get() = post.users
    override val datetime: String get() = ""
    override val speakerIds: List<Long> get() = listOf()
    override val participantsIds: List<Long> get() = listOf()
    override val participatedByMe: Boolean get() = false
    override val type: EventType get() = EventType.OFFLINE
    override val isPlayed: Boolean get() = post.isPlayed
    override val initInAudioPlayer: Boolean get() = post.initInAudioPlayer
}