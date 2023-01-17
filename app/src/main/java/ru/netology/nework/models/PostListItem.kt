package ru.netology.nework.models

data class PostListItem(
    val post: Post
) : java.io.Serializable {
    val id: Int get() = post.id
    val authorId: Int get() = post.authorId
    val author: String get() = post.author
    val authorAvatar: String? get() = post.authorAvatar
    val authorJob: String? get() = post.authorJob
    val content: String get() = post.content
    val published: String get() = post.published
    val coords: Coordinates? get() = post.coords
    val link: String? get() = post.link
    val likeOwnerIds: List<Int> get() = post.likeOwnerIds
    val mentionIds: List<Int> get() = post.mentionIds
    val mentionedMe: Boolean get() = post.mentionedMe
    val likedByMe: Boolean get() = post.likedByMe
    val attachment: Attachment? get() = post.attachment
    val ownedByMe: Boolean get() = post.ownedByMe
    val users: Map<Int, UserPreview> get() = post.users
}