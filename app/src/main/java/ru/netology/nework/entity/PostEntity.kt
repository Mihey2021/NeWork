package ru.netology.nework.entity

//TODO:
//@Entity
//data class PostEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int,
//    val authorId: Int,
//    val author: String,
//    val authorAvatar: String? = null,
//    val authorJob: String? = null,
//    val content: String,
//    val published: String,
//    val coords: Coordinates? = null,
//    val link: String? = null,
//    val likeOwnerIds: List<Int> = emptyList(),
//    val mentionIds: List<Int> = emptyList(),
//    val mentionedMe: Boolean,
//    val likedByMe: Boolean,
//    @Embedded
//    val attachment: AttachmentEmbeddable? = null,
//    val ownedByMe: Boolean,
//    val users: List<UserPreview> = emptyList(),
//) {
//    fun toDto() = Post(
//        id,
//        authorId,
//        author,
//        authorAvatar,
//        authorJob,
//        content,
//        published,
//        coords,
//        link,
//        likeOwnerIds,
//        mentionIds,
//        mentionedMe,
//        likedByMe,
//        attachment?.toDto(),
//        ownedByMe,
//        users
//    )
//
//    companion object {
//        fun fromDto(dto: Post) =
//            PostEntity(
//                dto.id,
//                dto.authorId,
//                dto.author,
//                dto.authorAvatar,
//                dto.authorJob,
//                dto.content,
//                dto.published,
//                dto.coords,
//                dto.link,
//                dto.likeOwnerIds,
//                dto.mentionIds,
//                dto.mentionedMe,
//                dto.likedByMe,
//                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
//                dto.ownedByMe,
//                dto.users
//            )
//    }
//}
//
//data class AttachmentEmbeddable(
//    var url: String,
//    var type: AttachmentType? = null,
//) {
//    fun toDto() = Attachment(url, type)
//
//    companion object {
//        fun fromDto(dto: Attachment?) = dto?.let {
//            AttachmentEmbeddable(it.url, it.type)
//        }
//    }
//}
//
//fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
//fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)
