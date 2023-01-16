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
import ru.netology.nework.view.loadCircleCrop

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onLikeLongClick(userIds: List<Int>) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onMention(post: Post) {}
    fun onPhotoView(photoUrl: String) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {
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

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            if (post.authorAvatar != null) avatar.loadCircleCrop(post.authorAvatar)
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
                    .load(post.attachment.url)
                    .placeholder(R.drawable.ic_baseline_loading_24)
                    .error(R.drawable.ic_baseline_non_loaded_image_24)
                    .timeout(10_000)
                    .into(attachmentImageView)

                attachmentImageView.setOnClickListener {
                    onInteractionListener.onPhotoView(post.attachment.url)
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
                onInteractionListener.onLikeLongClick(post.likeOwnerIds)
                showUsersPopupMenu(like, post.likeOwnerIds)
                return@setOnLongClickListener true
            }

            mention.setOnClickListener {
                onInteractionListener.onMention(post)
            }
        }
    }

    private fun showUsersPopupMenu(view: View, usersList: List<Int>) {
        val popupMenu = PopupMenu(view.context, view)
        usersList.forEach { popupMenu.menu.add(0, it, Menu.NONE, it.toString()) }

        popupMenu.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}
