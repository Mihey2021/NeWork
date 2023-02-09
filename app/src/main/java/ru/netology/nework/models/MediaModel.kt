package ru.netology.nework.models

import android.net.Uri
import okhttp3.MediaType
import java.io.File

data class MediaModel(
    val uri: Uri? = null,
    val fileDescription: Triple<File?, AttachmentType, MediaType?>? = null)