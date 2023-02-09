package ru.netology.nework.models

import okhttp3.MediaType
import java.io.File

data class Media(val url: String)

data class MediaUpload(val fileDescription: Triple<File, AttachmentType, MediaType?>)