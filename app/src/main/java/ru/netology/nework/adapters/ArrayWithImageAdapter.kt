package ru.netology.nework.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.netology.nework.R
import ru.netology.nework.databinding.EventTypeItemBinding
import ru.netology.nework.databinding.UserItemBinding
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.user.User
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.view.loadCircleCrop
import ru.netology.nework.view.loadFromResource

class ArrayWithImageAdapter<T>(
    @ApplicationContext
    context: Context,
    layout: Int,
    arrayData: Array<T>
) : ArrayAdapter<T>(context, layout, arrayData) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        val binding = if (item is EventType)
            EventTypeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        else
            UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        if (binding is EventTypeItemBinding) {
            with(binding) {
                textType.text = item.toString()
                AdditionalFunctions.setEventTypeColor(context, iconType, item as EventType)
            }
        }

        if (binding is UserItemBinding) {
            with(binding) {
                val dataItem = item as User
                userName.text = dataItem.name
                if (item.avatar != null)
                    avatar.loadCircleCrop(dataItem.avatar!!)
                else
                    avatar.loadFromResource(R.drawable.ic_baseline_account_circle_24)
            }
        }

        return binding.root
    }
}