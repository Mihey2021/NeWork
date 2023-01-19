package ru.netology.nework.utils

import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import ru.netology.nework.R
import ru.netology.nework.models.event.EventType

class AdditionalFunctions {
    companion object {
        fun setEventTypeColor(context: Context, view: ImageView, item: EventType) {
            if (item == EventType.ONLINE)
                view.setColorFilter(
                    ContextCompat.getColor(context, R.color.online_color),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            else
                view.setColorFilter(
                    ContextCompat.getColor(context, R.color.offline_color),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
        }
    }
}