package ru.netology.nework.adapters

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.PostCardBinding
import ru.netology.nework.models.AttachmentType
import ru.netology.nework.models.Post
import ru.netology.nework.models.PostListItem
import ru.netology.nework.models.UserPreview
import ru.netology.nework.view.load
import ru.netology.nework.view.loadCircleCrop
import ru.netology.nework.view.loadFromResource

interface OnInteractionListener {
    fun onLike(post: PostListItem) {}
    fun onLikeLongClick(view: View, post: PostListItem) {}
    fun onEdit(post: PostListItem) {}
    fun onRemove(post: PostListItem) {}
    fun onMention(post: PostListItem) {}
    fun onPhotoView(photoUrl: String) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<PostListItem, PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: PostCardBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: PostListItem) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            if (post.authorAvatar != null) avatar.loadCircleCrop(post.authorAvatar!!) else avatar.loadFromResource(R.drawable.ic_baseline_account_circle_24)
            coordinates.text = post.coords?.toString() ?: ""
            link.text = post.link
            like.isChecked = post.likedByMe
            like.text = "${post.likeOwnerIds.size}"
            mention.isChecked = post.mentionedMe
            mention.text = "${post.mentionIds.size}"

            menu.isVisible = post.ownedByMe

            val attachment = post.attachment
            if (attachment != null && attachment.type == AttachmentType.IMAGE) {
                attachmentImageView.visibility = View.VISIBLE

                Glide.with(attachmentImageView)
                    .load(post.attachment?.url)
                    .placeholder(R.drawable.ic_baseline_loading_24)
                    .error(R.drawable.ic_baseline_non_loaded_image_24)
                    .timeout(10_000)
                    .into(attachmentImageView)

                attachmentImageView.setOnClickListener {
                    onInteractionListener.onPhotoView(post.attachment?.url ?: "")
                }
            } else {
                attachmentImageView.visibility = View.GONE
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            like.setOnLongClickListener {
                onInteractionListener.onLikeLongClick(like, post)
//                showUsersPopupMenu(like, post.likeOwnerIds, post.users)
                return@setOnLongClickListener true
            }

            mention.setOnClickListener {
                onInteractionListener.onMention(post)
            }
        }
    }

//    private fun showUsersPopupMenu(view: View, usersList: List<Int>, users: Map<Int, UserPreview>) {
//        val popupMenu = PopupMenu(view.context, view)
//        usersList.forEach { popupMenu.menu.add(0, it, Menu.NONE, users[it]?.name ?: "<Undefined>") }
//
//        popupMenu.setOnMenuItemClickListener {
//            return@setOnMenuItemClickListener true
//        }
//        popupMenu.show()
//    }
}

class PostDiffCallback : DiffUtil.ItemCallback<PostListItem>() {
    override fun areItemsTheSame(oldItem: PostListItem, newItem: PostListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PostListItem, newItem: PostListItem): Boolean {
        return oldItem == newItem
    }
    //не применять анимацию (убрать "мерцание")
    override fun getChangePayload(oldItem: PostListItem, newItem: PostListItem): Any = Unit
}
