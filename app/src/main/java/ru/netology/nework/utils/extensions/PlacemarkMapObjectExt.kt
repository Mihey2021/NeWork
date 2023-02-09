package ru.netology.nework.utils.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider

fun PlacemarkMapObject.setIcon(drawable: Drawable) {
    val canvas = Canvas()
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    canvas.setBitmap(bitmap)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    drawable.draw(canvas)
    setIcon(ImageProvider.fromBitmap(bitmap))
}