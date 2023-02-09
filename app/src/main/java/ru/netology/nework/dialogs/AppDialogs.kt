package ru.netology.nework.dialogs

import android.annotation.SuppressLint

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nework.R

object AppDialogs {
    const val PROGRESS_DIALOG = 1
    const val QUESTION_DIALOG = 2
    const val ERROR_DIALOG = 3

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getDialog(
        context: Context,
        dialogId: Int,
        message: String = "",
        title: String = "",
        positiveButtonTitle: String? = null,
        negativeButtonTitle: String? = null,
        onDialogsInteractionListener: OnDialogsInteractionListener? = null,
        isCancelable: Boolean = false,
        titleIcon: Int = R.drawable.ic_baseline_error_24,
    ): AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(context)
        when (dialogId) {
            1 -> {
                with(alertDialog) {
                    setTitle(title)
                    setCancelable(isCancelable)
                    setView(ProgressBar(context).apply {
                        this.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setPadding(0, 52, 0, 52)
                        }
                    })
                }
            }
            2, 3 -> {
                with(alertDialog) {
                    setTitle(title)
                    setIcon(context.resources.getDrawable(
                        titleIcon,
                        context.theme
                    ))
                    setMessage(message)
                    setPositiveButton(
                        positiveButtonTitle
                            ?: context.resources.getString(android.R.string.ok)
                    ) { dialog, _ ->
                        onDialogsInteractionListener?.onPositiveClickButton()
                        dialog.dismiss()
                    }
                    if (dialogId == 2) {
                        setNegativeButton(
                            negativeButtonTitle ?: context.resources.getString(R.string.cancel_text)
                        ) { dialog, _ ->
                            onDialogsInteractionListener?.onNegativeClickButton()
                            dialog.cancel()
                        }
                    }
                }
            }
        }
        return alertDialog.show()
    }
}