package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
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
import ru.netology.nework.adapters.DataLoadingStateAdapter
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.adapters.PostsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.filter.Filters
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.DeepLinks
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.models.user.User
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private var dialog: AlertDialog? = null
    private var authUser: User? = null
    private lateinit var adapter: PostsAdapter
    private var filterBy: Long = 0L
    lateinit var binding: FragmentPostsBinding

    @Inject
    lateinit var appAuth: AppAuth

    @Inject
    lateinit var filters: Filters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentPostsBinding.inflate(layoutInflater)

        adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: DataItem) {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToNewPostFragment(editingData = post as PostListItem)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToNewPostFragment(
                            editingData = post as PostListItem
                        )
                findNavController().navigate(direction)
            }

            override fun onLike(post: DataItem) {
                if (!authViewModel.authorized)
                    showAuthorizationQuestionDialog()
                else {
                    viewModel.likeById(post.id, post.likedByMe)
                }
            }

            override fun onLikeLongClick(view: View, dataItem: DataItem) {
                val popupMenu = AdditionalFunctions.prepareUsersPopupMenu(
                    requireContext(),
                    view,
                    dataItem.likeOwnerIds,
                    dataItem.users,
                    authUser?.id ?: 0L
                )
                setListenersAndShowPopupMenu(popupMenu)
            }

            override fun onMentionClick(view: View, dataItem: DataItem) {
                val popupMenu = AdditionalFunctions.prepareUsersPopupMenu(
                    requireContext(),
                    view,
                    dataItem.mentionIds,
                    dataItem.users,
                    authUser?.id ?: 0L
                )
                setListenersAndShowPopupMenu(popupMenu)
            }

            override fun onRemove(post: DataItem) {
                viewModel.removeById(post.id)
            }

            override fun onCoordinatesClick(coordinates: Coordinates) {
                showMap(coordinates)
            }

            override fun onAvatarClick(authorId: Long) {
                findNavController().navigate(Uri.parse("${DeepLinks.USER_PAGE.link}${authorId}"))
            }

            override fun onPhotoView(photoUrl: String) {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToViewPhotoFragment(photoUrl)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToViewPhotoFragment(
                            photoUrl
                        )
                findNavController().navigate(direction)
            }

        })

        binding.postsList.adapter = adapter.withLoadStateHeaderAndFooter(
            header = DataLoadingStateAdapter(object :
                DataLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
            footer = DataLoadingStateAdapter(object :
                DataLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel.clearFeedModelState()

        lifecycleScope.launch {
            filters.filterBy.collectLatest { userId ->
                filterBy = userId
                setFabAddButtonVisibility(binding.fabPostAdd)
            }
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.needRefresh)
                adapter.refresh()
            if (state.error) {
                if (dialog?.isShowing == false || dialog == null) showErrorDialog(state.errorMessage)
            }
        }

        lifecycleScope.launch {
            viewModel.localDataFlow.collectLatest { pagedData ->
                adapter.submitData(pagedData)
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadState ->
                binding.swiperefresh.isRefreshing = loadState.refresh is LoadState.Loading
                        || loadState.append is LoadState.Loading
                        || loadState.prepend is LoadState.Loading

                when (val currentState = loadState.refresh) {
                    is LoadState.Error -> {
                        binding.progress.isVisible = false
                        val extractedException = currentState.error
                        if (dialog?.isShowing == false || dialog == null) showErrorDialog(
                            extractedException.message
                        )

                    }
                    LoadState.Loading -> {
                        binding.progress.isVisible = true
                    }
                    else -> {
                        binding.progress.isVisible = false
                        return@collectLatest
                    }
                }
            }
        }

        authViewModel.authData.observe(viewLifecycleOwner) {
            setFabAddButtonVisibility(binding.fabPostAdd)
            adapter.refresh()
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }

        binding.fabPostAdd.setOnClickListener {
            if (!authViewModel.authorized) {
                showAuthorizationQuestionDialog()
            } else {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToNewPostFragment(isNewPost = true)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToNewPostFragment(isNewPost = true)
                findNavController().navigate(direction)
            }
        }

        return binding.root
    }

    private fun setFabAddButtonVisibility(view: View) {
        view.isVisible = isNotAnotherUserPage(filterBy)
    }

    private fun isNotAnotherUserPage(filterBy: Long): Boolean {
        val authorizedUserId = appAuth.getAuthorizedUserId()
        return (((filterBy == authorizedUserId) && (authorizedUserId != 0L)) || ((filterBy == 0L) && (authorizedUserId != 0L)))
    }

    private fun setListenersAndShowPopupMenu(popupMenu: PopupMenu) {
        popupMenu.setOnMenuItemClickListener {
            //filters.setFilterBy(it.itemId.toLong())
            findNavController().navigate(Uri.parse("${DeepLinks.USER_PAGE.link}${it.itemId.toLong()}"))
            true
        }
        popupMenu.show()
    }

    private fun showMap(coordinates: Coordinates) {
        val direction =
            if (requireParentFragment() is FeedFragment)
                FeedFragmentDirections.actionFeedFragmentToMapFragment(
                    coordinates = coordinates,
                    readOnly = true
                )
            else
                UserPageFragmentDirections.actionUserPageFragmentToMapFragment(
                    coordinates = coordinates,
                    readOnly = true
                )
        findNavController().navigate(direction)
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