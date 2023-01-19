package ru.netology.nework.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.LoadStateBinding

class DataLoadingStateAdapter(
    private val onInteractionListener: OnInteractionListener,
) : LoadStateAdapter<DataLoadingStateAdapter.PostLoadingViewHolder>() {

    interface OnInteractionListener {
        fun onRetry() {}
    }

    override fun onBindViewHolder(holder: PostLoadingViewHolder, loadState: LoadState) {
        holder.bind(loadState = loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): PostLoadingViewHolder = PostLoadingViewHolder(
        LoadStateBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        onInteractionListener,
    )

    class PostLoadingViewHolder(
        private val loadStateBinding: LoadStateBinding,
        private val onInteractionListener: OnInteractionListener,
    ) : RecyclerView.ViewHolder(loadStateBinding.root) {
        fun bind(loadState: LoadState) {
            loadStateBinding.apply {
                progress.isVisible = loadState is LoadState.Loading
                retry.isVisible = loadState is LoadState.Error
                retry.setOnClickListener {
                    onInteractionListener.onRetry()
                }
            }
        }
    }
}