package ru.netology.nework.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.Serializable

object AndroidUtils {
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun <T : Serializable?> getSerializable(activity: Activity, name: String, clazz: Class<T>): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        activity.intent.getSerializableExtra(name, clazz)!!
    else
        activity.intent.getSerializableExtra(name) as T
}

fun <T : Serializable?> getSerializable(
    data: Bundle,
    name: String,
    clazz: Class<T>
): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        data.getSerializable(name, clazz)!!
    else
        data.getSerializable(name) as T
}