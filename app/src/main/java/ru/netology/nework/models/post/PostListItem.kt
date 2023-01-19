package ru.netology.nework.models.post

import ru.netology.nework.models.Attachment
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.user.UserPreview

data class PostListItem(
    val post: Post
) : java.io.Serializable {
    val id: Long get() = post.id
    val authorId: Long get() = post.authorId
    val author: String get() = post.author
    val authorAvatar: String? get() = post.authorAvatar
    val authorJob: String? get() = post.authorJob
    val content: String get() = post.content
    val published: String get() = post.published
    val coords: Coordinates? get() = post.coords
    val link: String? get() = post.link
    val likeOwnerIds: List<Long> get() = post.likeOwnerIds
    val mentionIds: List<Long> get() = post.mentionIds
    val mentionedMe: Boolean get() = post.mentionedMe
    val likedByMe: Boolean get() = post.likedByMe
    val attachment: Attachment? get() = post.attachment
    val ownedByMe: Boolean get() = post.ownedByMe
    val users: Map<Long, UserPreview> get() = post.users
}