package ru.netology.nework.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import ru.netology.nework.R
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.user.UserPreview
import java.text.SimpleDateFormat
import java.util.*

abstract class AdditionalFunctions {
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

        fun setMaterialButtonIconColor(view: MaterialButton, colorResource: Int) {
            view.setIconTintResource(colorResource)
        }

        fun prepareUsersPopupMenu(
            context: Context,
            view: View,
            usersIds: List<Long>,
            users: Map<Long, UserPreview>,
            authorizedUserId: Long
        ): PopupMenu {
            val popupMenu = PopupMenu(context, view)
            usersIds.forEach { userId ->
                popupMenu.menu.add(
                    0,
                    userId.toInt(),
                    Menu.NONE,
                    if (authorizedUserId == userId) context.getString(R.string.me_text) else users[userId]?.name
                        ?: context.getString(R.string.undefined)
                )
            }
            return popupMenu
        }

        fun getCurrentDateTime() =
            Calendar.getInstance().time

        @SuppressLint("SimpleDateFormat")
        fun getFormattedStringDateTime(
            stringDateTime: String,
            pattern: String = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'",
            patternTo: String = "dd.MM.yyyy HH:mm:ss",
            returnOriginalDateIfExceptException: Boolean = false,
        ): CharSequence {
            var formattedData = if (returnOriginalDateIfExceptException) stringDateTime else ""
            try {
                val sdf = SimpleDateFormat(pattern)
                //sdf.timeZone = TimeZone.getTimeZone("UTC")
                val strToDate = sdf.parse(stringDateTime)
                formattedData = SimpleDateFormat(patternTo).format(strToDate).toString()
            } catch (e: Exception) {
                //Из API внезапно пришла дата в другом формате - Можно записать в лог для анализа, например
            }
            return formattedData
        }

        @SuppressLint("SimpleDateFormat")
        fun getFormattedDateTimeToString(dateTime: Date, pattern: String = "dd.MM.yyyy"): String {
            val sdf = SimpleDateFormat(pattern)
            return sdf.format(dateTime).toString()
        }

        @SuppressLint("SimpleDateFormat")
        fun getFormattedDateTimeToInt(dateTime: Date, pattern: String): Int {
            val sdf = SimpleDateFormat(pattern)
            return sdf.format(dateTime).toInt()
        }

        @SuppressLint("SimpleDateFormat")
        fun convertStringDateToLong(date: String, pattern: String = "yyyy.MM.dd HH:mm"): Long {
            return try {
                val df = SimpleDateFormat(pattern)
                df.parse(date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        fun setFieldRequiredHint(textInput: TextInputLayout): CharSequence =
            "${textInput.hint} ${textInput.context.getString(R.string.required_field_tag)}"

        fun showErrorDialog(context: Context, message: String?) {
            AppDialogs.getDialog(
                context,
                AppDialogs.ERROR_DIALOG,
                title = context.getString(R.string.an_error_has_occurred),
                message = message ?: context.getString(R.string.an_error_has_occurred),
                titleIcon = R.drawable.ic_baseline_error_24,
                isCancelable = true
            )
        }

    }
}