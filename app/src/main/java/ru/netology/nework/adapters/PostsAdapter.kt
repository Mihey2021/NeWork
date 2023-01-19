package ru.netology.nework.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.R
import ru.netology.nework.databinding.PostCardBinding
import ru.netology.nework.models.*
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.view.loadCircleCrop
import ru.netology.nework.view.loadFromResource
import java.text.SimpleDateFormat
import java.util.*

interface OnInteractionListener {
    fun onLike(post: DataItem) {}
    fun onLikeLongClick(view: View, post: DataItem) {}
    fun onEdit(post: DataItem) {}
    fun onRemove(post: DataItem) {}
    fun onMention(post: DataItem) {}
    fun onPhotoView(photoUrl: String) {}
    fun onCoordinatesClick(coordinates: Coordinates) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<ru.netology.nework.models.post.PostListItem, ViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post)
    }
}

class EventsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<EventListItem, ViewHolder>(EventDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position) ?: return
        holder.bind(event)
    }
}

class ViewHolder(
    private val binding: PostCardBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun <T : DataItem> bind(dataItem: T) {
        binding.apply {
            author.text = dataItem.author
            published.text = getFormattedDate(dataItem.published)
            content.text = dataItem.content
            if (dataItem.authorAvatar != null) avatar.loadCircleCrop(dataItem.authorAvatar!!) else avatar.loadFromResource(
                R.drawable.ic_baseline_account_circle_24
            )
            if (dataItem.coords != null) {
                coordinates.text = dataItem.coords.toString()
                coordinates.setOnClickListener {
                    onInteractionListener.onCoordinatesClick(dataItem.coords!!)
                }
                coordinates.visibility = View.VISIBLE
            } else {
                coordinates.visibility = View.GONE
            }
            link.text = dataItem.link
            like.isChecked = dataItem.likedByMe
            like.text = "${dataItem.likeOwnerIds.size}"
            mention.isChecked = dataItem.mentionedMe
            mention.text = "${dataItem.mentionIds.size}"

            menu.isVisible = dataItem.ownedByMe

            val attachment = dataItem.attachment
            if (attachment != null && attachment.type == AttachmentType.IMAGE) {
                attachmentImageView.visibility = View.VISIBLE

                Glide.with(attachmentImageView)
                    .load(dataItem.attachment?.url)
                    .placeholder(R.drawable.ic_baseline_loading_24)
                    .error(R.drawable.ic_baseline_non_loaded_image_24)
                    .timeout(10_000)
                    .into(attachmentImageView)

                attachmentImageView.setOnClickListener {
                    onInteractionListener.onPhotoView(dataItem.attachment?.url ?: "")
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
                                onInteractionListener.onRemove(dataItem)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(dataItem)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(dataItem)
            }

            like.setOnLongClickListener {
                onInteractionListener.onLikeLongClick(like, dataItem)
                return@setOnLongClickListener true
            }

            mention.setOnClickListener {
                onInteractionListener.onMention(dataItem)
            }

            if (dataItem is EventListItem) {
                eventDetailGroup.visibility = View.VISIBLE
                eventDate.text = getFormattedDate(dataItem.datetime)
                AdditionalFunctions.setEventTypeColor(iconType.context, iconType, dataItem.type)
                textType.text = dataItem.type.toString()
            } else {
                eventDetailGroup.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFormattedDate(stringDateTime: String): CharSequence {
        var formattedData = stringDateTime
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val strToDate = sdf.parse(stringDateTime)
            formattedData = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(strToDate).toString()
        } catch (e: Exception) {
            //Из API внезапно пришла дата в другом формате - Можно записать в лог для анализа
        }
        return formattedData
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<ru.netology.nework.models.post.PostListItem>() {
    override fun areItemsTheSame(
        oldItem: ru.netology.nework.models.post.PostListItem,
        newItem: ru.netology.nework.models.post.PostListItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ru.netology.nework.models.post.PostListItem,
        newItem: ru.netology.nework.models.post.PostListItem
    ): Boolean {
        return oldItem == newItem
    }

    //не применять анимацию (убрать "мерцание")
    override fun getChangePayload(
        oldItem: ru.netology.nework.models.post.PostListItem,
        newItem: ru.netology.nework.models.post.PostListItem
    ): Any = Unit
}

class EventDiffCallback : DiffUtil.ItemCallback<EventListItem>() {
    override fun areItemsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean {
        return oldItem == newItem
    }

    //не применять анимацию (убрать "мерцание")
    override fun getChangePayload(oldItem: EventListItem, newItem: EventListItem): Any = Unit
}
