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
import ru.netology.nework.adapters.EventsAdapter
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.AudioPlayerBinding
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.databinding.VideoPlayerBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.filter.Filters
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.DeepLinks
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.mediaPlayers.CustomMediaPlayer
import ru.netology.nework.models.user.User
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.utils.AdditionalFunctions.Companion.showErrorDialog
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment(R.layout.fragment_events) {

    private val viewModel: EventViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    @Inject
    lateinit var filters: Filters

    @Inject
    lateinit var customMediaPlayer: CustomMediaPlayer

    private var dialog: AlertDialog? = null
    private var authUser: User? = null
    private var filterBy: Long = 0L
    private lateinit var binding: FragmentEventsBinding
    private lateinit var adapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentEventsBinding.inflate(layoutInflater)

        adapter = EventsAdapter(object : OnInteractionListener {
            override fun onEdit(dataItem: DataItem) {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToNewPostFragment(editingData = dataItem as EventListItem)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToNewPostFragment(
                            editingData = dataItem as EventListItem
                        )
                findNavController().navigate(direction)
            }

            override fun onLike(dataItem: DataItem) {
                if (!authViewModel.authorized)
                    showAuthorizationQuestionDialog()
                else {
                    viewModel.likeById(dataItem.id, dataItem.likedByMe)
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

            override fun onSpeakerClick(view: View, dataItem: DataItem) {
                val popupMenu = AdditionalFunctions.prepareUsersPopupMenu(
                    requireContext(),
                    view,
                    dataItem.speakerIds,
                    dataItem.users,
                    authUser?.id ?: 0L
                )
                setListenersAndShowPopupMenu(popupMenu)
            }

            override fun onParticipantsClick(eventId: Long, participatedByMe: Boolean) {
                if (participatedByMe)
                    viewModel.removeParticipant(eventId)
                else
                    viewModel.setParticipant(eventId)
            }

            override fun onParticipantsLongClick(view: View, dataItem: DataItem) {
                if (!authViewModel.authorized)
                    showAuthorizationQuestionDialog()
                else {
                    val popupMenu = AdditionalFunctions.prepareUsersPopupMenu(
                        requireContext(),
                        view,
                        dataItem.participantsIds,
                        dataItem.users,
                        appAuth.getAuthorizedUserId()
                    )
                    setListenersAndShowPopupMenu(popupMenu)
                }
            }

            override fun onRemove(dataItem: DataItem) {
                viewModel.removeById(dataItem.id)
            }

            override fun onCoordinatesClick(coordinates: Coordinates) {
                showMap(coordinates)
            }

            override fun onAvatarClick(authorId: Long) {
                findNavController().navigate(Uri.parse("${DeepLinks.USER_PAGE.link}${authorId}"))
            }

            override fun onPlayStopMedia(dataItem: DataItem, binding: AudioPlayerBinding) {
                stopPreviousMedia()
                customMediaPlayer.playStopAudio(dataItem, binding)
            }

            override fun onPlayStopMedia(dataItem: DataItem, binding: VideoPlayerBinding) {
                stopPreviousMedia()
                customMediaPlayer.playStopVideo(dataItem, binding)
            }

            override fun onFullScreenVideo(dataItem: DataItem) {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToVideoFragment(dataItem)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToVideoFragment(
                            dataItem
                        )
                findNavController().navigate(direction)
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
        }, customMediaPlayer)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel.clearFeedModelState()

        lifecycleScope.launch {
            customMediaPlayer.mediaPlayerStateChange.collectLatest {
                if (it == null) return@collectLatest
                if (it is EventListItem)
                    viewModel.playStopMedia(it.event)
            }
        }

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

        lifecycleScope.launch {
            filters.filterBy.collectLatest { userId ->
                filterBy = userId
                setFabAddButtonVisibility(binding.fabEventAdd)
            }
        }

        authViewModel.authData.observe(viewLifecycleOwner) {
            setFabAddButtonVisibility(binding.fabEventAdd)
            adapter.refresh()
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.needRefresh)
                adapter.refresh()
            if (state.error) {
                if (dialog?.isShowing == false || dialog == null) showErrorDialog(
                    requireContext(),
                    state.errorMessage
                )
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
                            requireContext(),
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
            adapter.refresh()
        }

        authViewModel.authUser.observe(viewLifecycleOwner) {
            authUser = it
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }


        binding.fabEventAdd.setOnClickListener {
            if (!authViewModel.authorized) {
                showAuthorizationQuestionDialog()
            } else {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToNewPostFragment(isNewEvent = true)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToNewPostFragment(
                            isNewEvent = true
                        )
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

    private fun stopPreviousMedia() {
        val event = viewModel.getMediaPlayingEvent()
        if (event != null) {
            customMediaPlayer.stopMediaPlaying(EventListItem(event = event) as DataItem)
        }
    }

    override fun onPause() {
        stopPreviousMedia()
        super.onPause()
    }
}