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
import ru.netology.nework.databinding.AudioPlayerBinding
import ru.netology.nework.databinding.PostCardBinding
import ru.netology.nework.databinding.VideoPlayerBinding
import ru.netology.nework.extensions.loadCircleCrop
import ru.netology.nework.extensions.loadFromResource
import ru.netology.nework.models.*
import ru.netology.nework.models.mediaPlayers.CustomMediaPlayer
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.utils.AdditionalFunctions
import java.util.*
import javax.inject.Inject

interface OnInteractionListener {
    fun onLike(dataItem: DataItem) {}
    fun onLikeLongClick(view: View, dataItem: DataItem) {}
    fun onMentionClick(view: View, dataItem: DataItem) {}
    fun onSpeakerClick(view: View, dataItem: DataItem) {}
    fun onParticipantsClick(eventId: Long, participatedByMe: Boolean) {}
    fun onParticipantsLongClick(view: View, dataItem: DataItem) {}
    fun onEdit(dataItem: DataItem) {}
    fun onRemove(dataItem: DataItem) {}
    fun onPhotoView(photoUrl: String) {}
    fun onCoordinatesClick(coordinates: Coordinates) {}
    fun onAvatarClick(authorId: Long) {}
    fun onPlayStopMedia(dataItem: DataItem, binding: AudioPlayerBinding) {}
    fun onPlayStopMedia(dataItem: DataItem, binding: VideoPlayerBinding) {}
    fun onFullScreenVideo(dataItem: DataItem) {}
}

class PostsAdapter @Inject constructor(
    private val onInteractionListener: OnInteractionListener,
    private val customMediaPlayer: CustomMediaPlayer,
) : PagingDataAdapter<PostListItem, ViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onInteractionListener, customMediaPlayer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post)
    }
}

class EventsAdapter @Inject constructor(
    private val onInteractionListener: OnInteractionListener,
    private val customMediaPlayer: CustomMediaPlayer,
) : PagingDataAdapter<EventListItem, ViewHolder>(EventDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onInteractionListener, customMediaPlayer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position) ?: return
        holder.bind(event)
    }
}

class ViewHolder @Inject constructor(
    private val binding: PostCardBinding,
    private val onInteractionListener: OnInteractionListener,
    private val customMediaPlayer: CustomMediaPlayer,
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

            audioPlayerContainer.visibility = View.GONE
            attachmentImageView.visibility = View.GONE
            videoPlayerContainer.visibility = View.GONE

            val playing = dataItem.isPlayed
            audioPlayerInclude.playStop.isChecked = playing
            videoPlayerInclude.playStop.isChecked = playing
            if (!playing) {
                audioPlayerInclude.duration.text = ""
                audioPlayerInclude.currentPosition.text = ""
                audioPlayerInclude.seekBar.max = 0
                audioPlayerInclude.seekBar.progress = 0
            } else {
                customMediaPlayer.refreshSeekBar(binding.audioPlayerInclude)
            }

            val attachment = dataItem.attachment
            if (attachment != null) {
                when (attachment.type) {
                    AttachmentType.IMAGE -> {
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
                    }
                    AttachmentType.AUDIO -> {
                        audioPlayerContainer.visibility = View.VISIBLE
                        audioPlayerInclude.playStop.setOnClickListener {
                            onInteractionListener.onPlayStopMedia(
                                dataItem,
                                binding.audioPlayerInclude
                            )
                        }
                    }
                    else -> {
                        videoPlayerContainer.visibility = View.VISIBLE
                        videoPlayerInclude.playStop.setOnClickListener {
                            onInteractionListener.onPlayStopMedia(
                                dataItem,
                                binding.videoPlayerInclude
                            )
                        }
                        videoPlayerInclude.fullScreen.setOnClickListener {
                            onInteractionListener.onFullScreenVideo(dataItem)
                        }
                    }
                }
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

class PostDiffCallback : DiffUtil.ItemCallback<PostListItem>() {
    override fun areItemsTheSame(
        oldItem: PostListItem,
        newItem: PostListItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: PostListItem,
        newItem: PostListItem
    ): Boolean {
        return oldItem == newItem
    }

    //не применять анимацию (убрать "мерцание")
    override fun getChangePayload(
        oldItem: PostListItem,
        newItem: PostListItem
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
