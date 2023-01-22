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
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.view.loadCircleCrop
import ru.netology.nework.view.loadFromResource
import java.util.*

interface OnInteractionListener {
    fun onLike(post: DataItem) {}
    fun onLikeLongClick(view: View, dataItem: DataItem) {}
    fun onMentionClick(view: View, dataItem: DataItem) {}
    fun onSpeakerClick(view: View, dataItem: DataItem) {}
    fun onParticipantsClick(eventId: Long, participatedByMe: Boolean) {}
    fun onParticipantsLongClick(view: View, dataItem: DataItem) {}
    fun onEdit(post: DataItem) {}
    fun onRemove(post: DataItem) {}
    fun onPhotoView(photoUrl: String) {}
    fun onCoordinatesClick(coordinates: Coordinates) {}
    fun onAvatarClick(authorId: Long)
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
    private var additionalText: String = ""

    @SuppressLint("SetTextI18n")
    fun <T : DataItem> bind(dataItem: T) {
        binding.apply {
            author.text = dataItem.author
            published.text = AdditionalFunctions.getFormattedStringDateTime(
                stringDateTime = dataItem.published,
                returnOriginalDateIfExceptException = true
            )
            content.text = dataItem.content
            if (dataItem.authorAvatar != null) avatar.loadCircleCrop(dataItem.authorAvatar!!) else avatar.loadFromResource(
                R.drawable.ic_baseline_account_circle_24
            )

            avatar.setOnClickListener {
                onInteractionListener.onAvatarClick(dataItem.authorId)
            }

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
            like.text = dataItem.likeOwnerIds.count().toString()

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
                            R.id.content -> {
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

            mention.isChecked = dataItem.mentionedMe
            mention.text = "${dataItem.mentionIds.size}"
            AdditionalFunctions.setMaterialButtonIconColor(mention, R.color.gray)
            if (dataItem.mentionIds.isNotEmpty()) {
                additionalText = mention.text.toString()
                if (dataItem.mentionedMe) {
                    additionalText += " (${mention.context.getString(R.string.you_have_been_marked)})"
                    AdditionalFunctions.setMaterialButtonIconColor(
                        mention,
                        R.color.green
                    )
                } else {
                    AdditionalFunctions.setMaterialButtonIconColor(
                        mention,
                        R.color.blue
                    )
                }
                mention.text = additionalText
                mention.setOnClickListener {
                    onInteractionListener.onMentionClick(it, dataItem)
                }
            }

            speakers.text = dataItem.speakerIds.count().toString()
            participants.text = dataItem.participantsIds.count().toString()

            if (dataItem is EventListItem) {
                eventDetailGroup.visibility = View.VISIBLE
                eventDate.text = AdditionalFunctions.getFormattedStringDateTime(
                    stringDateTime = dataItem.datetime,
                    returnOriginalDateIfExceptException = true
                )
                AdditionalFunctions.setEventTypeColor(iconType.context, iconType, dataItem.type)
                textType.text = dataItem.type.toString()
                mention.visibility = View.GONE
                speakers.visibility = View.VISIBLE
                speakers.setOnClickListener {
                    onInteractionListener.onSpeakerClick(it, dataItem)
                }
                participants.visibility = View.VISIBLE

                participants.setOnClickListener {
                    onInteractionListener.onParticipantsClick(
                        dataItem.id,
                        dataItem.participatedByMe
                    )
                }
                participants.setOnLongClickListener {
                    onInteractionListener.onParticipantsLongClick(it, dataItem)
                    true
                }
            } else {
                mention.visibility = View.VISIBLE
                eventDetailGroup.visibility = View.GONE
                speakers.visibility = View.GONE
                participants.visibility = View.GONE
            }
        }
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
