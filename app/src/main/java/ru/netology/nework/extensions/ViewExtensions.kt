package ru.netology.nework.extensions

import android.annotation.SuppressLint
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop

fun ImageView.load(url: String, vararg transforms: BitmapTransformation = emptyArray()) =
    Glide.with(this)
        .load(url)
        .timeout(10_000)
        .transform(*transforms)
        .into(this)

fun ImageView.loadCircleCrop(url: String, vararg transforms: BitmapTransformation = emptyArray()) =
    load(url, CircleCrop(), *transforms)

@SuppressLint("UseCompatLoadingForDrawables")
fun ImageView.loadFromResource(
    resource: Int,
    vararg transforms: BitmapTransformation = emptyArray()
) =
    Glide.with(this)
        .load(context.getDrawable(resource))
        .timeout(10_000)
        .transform(*transforms)
        .into(this)