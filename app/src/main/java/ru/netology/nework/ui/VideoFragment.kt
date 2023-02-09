package ru.netology.nework.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.VideoPlayerBinding
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.mediaPlayers.CustomMediaPlayer
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.viewmodels.EventViewModel
import ru.netology.nework.viewmodels.PostViewModel
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class VideoFragment : Fragment(R.layout.video_player) {

    private val postViewModel: PostViewModel by activityViewModels()
    private val eventViewModel: EventViewModel by activityViewModels()

    @Inject
    lateinit var customMediaPlayer: CustomMediaPlayer

    private var data: DataItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = VideoPlayerBinding.inflate(inflater, container, false)

        data = arguments?.customGetSerializable<DataItem>("dataItem")
        if (data != null) {
            customMediaPlayer.playStopVideo(dataItem = data, binding = binding, isFullScreen = true)
        }

        return binding.root
    }

    private fun stopMediaPlayed() {
        if (data == null) return
        val dataItem =
            if (data is PostListItem) postViewModel.getMediaPlayingPost() else eventViewModel.getMediaPlayingEvent()
        if (dataItem != null)
            customMediaPlayer.stopMediaPlaying(if(dataItem is Post) PostListItem(post = dataItem) else EventListItem(event = dataItem as Event))

    }

    override fun onPause() {
        stopMediaPlayed()
        super.onPause()
    }

}

inline fun <reified T : Serializable> Bundle.customGetSerializable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        getSerializable(key) as? T
    }
}