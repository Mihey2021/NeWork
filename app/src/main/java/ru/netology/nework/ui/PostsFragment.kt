package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.adapters.PostsAdapter
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.Post
import ru.netology.nework.models.User
import ru.netology.nework.viewmodels.PostViewModel

@AndroidEntryPoint
class PostsFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private var dialog: AlertDialog? = null
    private var authUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostsBinding.inflate(inflater)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                //viewModel.edit(PostCreated(post.id, post.content, post.coords, post.link, post.attachment, post.mentionIds))
                val direction = FeedFragmentDirections.actionFeedFragmentToNewPostFragment(post)
                findNavController().navigate(direction)
            }

            override fun onLike(post: Post) {
                if (!viewModel.authorized)
                    showAuthorizationQuestionDialog()
                else
                    viewModel.likeById(post.id, post.likedByMe)
            }

            override fun onLikeLongClick(userIds: List<Int>) {
                Toast.makeText(requireContext(), userIds.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }
        })

        binding.postsList.adapter = adapter

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                showErrorDialog(state.errorMessage)
            }
        }

        lifecycleScope.launch {
            viewModel.data.collectLatest { pagedData ->
                adapter.submitData(pagedData)
            }
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadState ->
                when (val currentState = loadState.refresh) {
                    is LoadState.Error -> {
                        val extractedException = currentState.error
                        showErrorDialog(extractedException.message)
                    }
                    else -> return@collectLatest
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest {
                binding.swiperefresh.isRefreshing = it.refresh is LoadState.Loading
                        || it.append is LoadState.Loading
                        || it.prepend is LoadState.Loading
            }
        }

        viewModel.authData.observe(viewLifecycleOwner) {
            if (it != null) viewModel.getUserById(it.id) else setActionBarSubTitle()
            adapter.refresh()
        }

        viewModel.authUser.observe(viewLifecycleOwner) {
            authUser = it
            setActionBarSubTitle(it?.name)
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }


        binding.fabPostAdd.setOnClickListener {
            if (!viewModel.authorized)
                showAuthorizationQuestionDialog()
            else
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }

    private fun showErrorDialog(message: String?) {
        dialog = AppDialogs.getDialog(
            requireContext(),
            AppDialogs.ERROR_DIALOG,
            title = getString(R.string.an_error_has_occurred),
            message = message ?: getString(R.string.an_error_has_occurred),
            titleIcon = R.drawable.ic_baseline_error_24,
            isCancelable = true
        )
    }

    private fun setActionBarSubTitle(subTitle: String? = null) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.subtitle = subTitle ?: ""
    }

    private fun showAuthorizationQuestionDialog() {
        AppDialogs.getDialog(requireContext(),
            AppDialogs.QUESTION_DIALOG,
            title = getString(R.string.authorization),
            message = getString(R.string.do_you_want_to_login),
            titleIcon = R.drawable.ic_baseline_lock_24,
            positiveButtonTitle = getString(R.string.yes_text),
            onDialogsInteractionListener = object : OnDialogsInteractionListener {
                override fun onPositiveClickButton() {
                    findNavController().navigate(R.id.action_feedFragment_to_authFragment)
                }
            })
    }
}