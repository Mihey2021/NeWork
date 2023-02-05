package ru.netology.nework.models

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.R
import ru.netology.nework.databinding.PostCardBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.post.PostListItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private var audioJob: Job? = null
private var mediaPlayer: MediaPlayer? = MediaPlayer()
private var dialog: AlertDialog? = null

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    private val _audioPlayerStateChange: MutableStateFlow<DataItem?> = MutableStateFlow(null)
    val audioPlayerStateChange = _audioPlayerStateChange.asStateFlow()

    lateinit var audioBinding: PostCardBinding

    fun playStopAudio(dataItem: DataItem, binding: PostCardBinding) {
        audioBinding = binding
        audioJob?.cancel()
        if (!dataItem.isAudioPlayed) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener {
                stopPlaying(dataItem)
            }
            mediaPlayer?.setDataSource(dataItem.attachment!!.url)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                it.start()
                _audioPlayerStateChange.value = getNewDataItem(dataItem, isStopped = false)
                initializeSeekBar()
            }
            return
        }

        stopPlaying(dataItem)
        initializeSeekBar()
    }

    fun stopPlaying(dataItem: DataItem) {
        audioJob?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _audioPlayerStateChange.value = getNewDataItem(dataItem)
    }

    private fun getNewDataItem(dataItem: DataItem, isStopped: Boolean = true): DataItem {
        return if (dataItem is PostListItem)
            dataItem.copy(post = dataItem.post.copy(isAudioPlayed = !isStopped))
        else
            (dataItem as EventListItem).copy(event = dataItem.event.copy(isAudioPlayed = !isStopped))
    }

    @SuppressLint("SetTextI18n")
    private fun initializeSeekBar() {
        if (mediaPlayer != null) {
            audioBinding.audioPlayerInclude.duration.text =
                getFormattingTimeString(mediaPlayer!!.duration)
        } else {
            audioBinding.audioPlayerInclude.duration.text = ""
        }
        audioBinding.audioPlayerInclude.currentPosition.text = ""
        val seekBar = audioBinding.audioPlayerInclude.seekBar
        seekBar.max = mediaPlayer?.duration ?: 0
        seekBar.progress = mediaPlayer?.currentPosition ?: 0
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
                            seekBar.progress = currentPosition
                            audioBinding.audioPlayerInclude.currentPosition.text =
                               if (mediaPlayer == null) "" else getCurrentPositionText(currentPosition)
                        }
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                showErrorDialog(e.message.toString())
            }
        }
    }

    fun refreshSeekBar(binding: PostCardBinding) {
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