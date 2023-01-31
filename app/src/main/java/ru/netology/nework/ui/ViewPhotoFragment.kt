package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentViewPhotoBinding

class ViewPhotoFragment: Fragment(R.layout.fragment_view_photo) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentViewPhotoBinding.inflate(inflater, container, false)

        val photoUrl = arguments?.getString("photoUrl")

        val attachmentImageView = binding.attachmentImageView
        Glide.with(attachmentImageView)
            .load(photoUrl)
            .placeholder(R.drawable.ic_baseline_loading_24)
            .error(R.drawable.ic_baseline_non_loaded_image_24)
            .timeout(10_000)
            .into(attachmentImageView)

        return binding.root

    }
}