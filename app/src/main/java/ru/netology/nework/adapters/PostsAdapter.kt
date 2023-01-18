package ru.netology.nework.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
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
import ru.netology.nework.models.*
import ru.netology.nework.view.loadCircleCrop
import ru.netology.nework.view.loadFromResource
import java.text.SimpleDateFormat
import java.util.*

interface OnInteractionListener {
    fun onLike(post: PostListItem) {}
    fun onLikeLongClick(view: View, post: PostListItem) {}
    fun onEdit(post: PostListItem) {}
    fun onRemove(post: PostListItem) {}
    fun onMention(post: PostListItem) {}
    fun onPhotoView(photoUrl: String) {}
    fun onCoordinatesClick(coordinates: Coordinates) {}
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
            published.text = getFormattedDate(post.published)
            content.text = post.content
            if (post.authorAvatar != null) avatar.loadCircleCrop(post.authorAvatar!!) else avatar.loadFromResource(
                R.drawable.ic_baseline_account_circle_24
            )
            if (post.coords != null) {
                coordinates.text = post.coords.toString()
                coordinates.setOnClickListener {
                    onInteractionListener.onCoordinatesClick(post.coords!!)
                }
                coordinates.visibility = View.VISIBLE
            } else {
                coordinates.visibility = View.GONE
            }
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

    @SuppressLint("SimpleDateFormat")
    private fun getFormattedDate(published: String): CharSequence {
        var formattedData = published
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val strToDate = sdf.parse(published)
            formattedData = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(strToDate).toString()
        } catch (e: Exception) {
            //Из API внезапно пришла дата в другом формате - Можно записать в лог для анализа
        }
        return formattedData
    }
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
