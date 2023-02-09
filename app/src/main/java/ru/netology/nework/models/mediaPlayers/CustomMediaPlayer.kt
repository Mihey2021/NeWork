package ru.netology.nework.models.mediaPlayers

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.R
import ru.netology.nework.databinding.AudioPlayerBinding
import ru.netology.nework.databinding.VideoPlayerBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.post.PostListItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private var audioJob: Job? = null
private var mediaPlayer: MediaPlayer? = MediaPlayer()
private var videoView: VideoView? = null
private var dialog: AlertDialog? = null

@Singleton
class CustomMediaPlayer @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    private val _mediaPlayerStateChange: MutableStateFlow<DataItem?> = MutableStateFlow(null)
    val mediaPlayerStateChange = _mediaPlayerStateChange.asStateFlow()

    private var audioBinding: AudioPlayerBinding? = null
    private var videoBinding: VideoPlayerBinding? = null

    fun playStopAudio(
        dataItem: DataItem? = null,
        binding: AudioPlayerBinding,
        newMediaAttachment: NewMediaAttachment? = null
    ) {

        if (dataItem == null && newMediaAttachment == null) return

        audioBinding = binding
        audioJob?.cancel()
        val playing = newMediaAttachment?.nowPlaying ?: dataItem!!.isPlayed
        if (!playing) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener {
                stopMediaPlaying(dataItem)
            }
            mediaPlayer?.setDataSource(newMediaAttachment?.url ?: dataItem!!.attachment!!.url)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                it.start()
                if (dataItem != null)
                    _mediaPlayerStateChange.value = getNewDataItem(dataItem, isStopped = false)
                initializeSeekBar()
            }
            return
        }

        stopMediaPlaying(dataItem)
        initializeSeekBar()
    }

    fun playStopVideo(
        dataItem: DataItem? = null,
        binding: VideoPlayerBinding,
        newMediaAttachment: NewMediaAttachment? = null,
        isFullScreen: Boolean = false,
    ) {
        videoBinding = binding
        videoView = videoBinding?.videoView
        val playing = newMediaAttachment?.nowPlaying ?: dataItem!!.isPlayed
        videoBinding!!.playStop.isChecked = !playing
        if (!playing || isFullScreen) {
            if (isFullScreen) {
                videoBinding!!.playStop.visibility = View.GONE
                videoBinding!!.fullScreen.visibility = View.GONE
                val mediaController = MediaController(videoBinding!!.videoView.context)
                mediaController.setAnchorView(videoView)
                videoView?.setMediaController(mediaController)
            }
            val uri = Uri.parse(newMediaAttachment?.url ?: dataItem!!.attachment!!.url)
            videoView?.setVideoURI(uri)
            videoBinding!!.videoLoadingProgress.visibility = View.VISIBLE
            videoView?.requestFocus()
            videoView?.setOnPreparedListener {
                videoBinding!!.videoLoadingProgress.visibility = View.GONE
                videoView?.start()
                if (dataItem != null)
                    _mediaPlayerStateChange.value = getNewDataItem(dataItem, isStopped = false)
            }
        } else {
            stopMediaPlaying(dataItem)
        }
    }

    fun stopMediaPlaying(dataItem: DataItem? = null, onlyStop: Boolean = false) {
        audioJob?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        videoView?.stopPlayback()
        videoView = null
        initializeSeekBar()
        if (onlyStop) {
            if (videoBinding != null) videoBinding!!.playStop.isChecked = false
            return
        }
        if (dataItem != null) {
            _mediaPlayerStateChange.value = getNewDataItem(dataItem, isStopped = true)
        }
    }

    private fun getNewDataItem(dataItem: DataItem, isStopped: Boolean = true): DataItem {
        return if (dataItem is PostListItem)
            dataItem.copy(post = dataItem.post.copy(isPlayed = !isStopped))
        else
            (dataItem as EventListItem).copy(event = dataItem.event.copy(isPlayed = !isStopped))
    }

    @SuppressLint("SetTextI18n")
    private fun initializeSeekBar() {
        if (mediaPlayer != null) {
            audioBinding?.playStop?.isChecked = true
            audioBinding?.duration?.text =
                getFormattingTimeString(mediaPlayer!!.duration)
        } else {
            audioBinding?.playStop?.isChecked = false
            audioBinding?.duration?.text = ""
        }
        audioBinding?.currentPosition?.text = ""
        val seekBar = audioBinding?.seekBar
        seekBar?.max = mediaPlayer?.duration ?: 0
        seekBar?.progress = mediaPlayer?.currentPosition ?: 0
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        with(CoroutineScope(EmptyCoroutineContext)) {
            try {
                audioJob = launch {
                    val isPlaying = mediaPlayer?.isPlaying ?: false
                    while (isPlaying) {
                        withContext(Dispatchers.Main) {
                            val currentPosition = mediaPlayer?.currentPosition ?: 0
                            seekBar?.progress = currentPosition
                            audioBinding?.currentPosition?.text =
                                if (mediaPlayer == null) "" else getCurrentPositionText(
                                    currentPosition
                                )
                        }
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                showErrorDialog(e.message.toString())
            }
        }
    }

    fun refreshSeekBar(binding: AudioPlayerBinding) {
        audioBinding = binding
        initializeSeekBar()
    }

    private fun getCurrentPositionText(currentPosition: Int): CharSequence =
        if (currentPosition == 0) "0:00" else getFormattingTimeString(currentPosition)

    private fun getFormattingTimeString(duration: Int): String {
        val min = duration / 1000 / 60
        val sec = duration / 1000 % 60
        return "$min:${if (sec == 0) "00" else if (sec > 9) sec else "0$sec"}"
    }

    private fun showErrorDialog(message: String?) {
        dialog = AppDialogs.getDialog(
            context,
            AppDialogs.ERROR_DIALOG,
            title = context.getString(R.string.an_error_has_occurred),
            message = message ?: context.getString(R.string.an_error_has_occurred),
            titleIcon = R.drawable.ic_baseline_error_24,
            isCancelable = true
        )
    }

}