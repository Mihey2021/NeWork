package ru.netology.nework.models.mediaPlayers

import android.content.Context
import android.net.Uri
import android.widget.MediaController
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.databinding.VideoPlayerBinding
import ru.netology.nework.models.DataItem
import javax.inject.Inject
import javax.inject.Singleton

lateinit var mediaController: MediaController

@Singleton
class VideoPlayer @Inject constructor(
    @ApplicationContext
    context: Context,
){

    init {
        //mediaController = MediaController(context)
    }

    fun playStopVideo(
        dataItem: DataItem? = null,
        binding: VideoPlayerBinding,
        newMediaAttachment: NewMediaAttachment? = null
    ){
        val videoView = binding.videoView
        val mediaController = MediaController(binding.videoView.context)
        val playing = newMediaAttachment?.nowPlaying ?: dataItem!!.isPlayed
        if(!playing) {
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            val uri = Uri.parse(newMediaAttachment?.url ?: dataItem!!.attachment!!.url)
            videoView.setVideoURI(uri)
            videoView.requestFocus()
            videoView.start()
        } else
        {
            videoView.stopPlayback()
        }
    }

}