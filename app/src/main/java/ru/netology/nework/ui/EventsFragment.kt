package ru.netology.nework.ui

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
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.filter.Filters
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.event.EventListItem
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

    private var dialog: AlertDialog? = null
    private var authUser: User? = null
    private var filterBy: Long = 0L
    private lateinit var binding: FragmentEventsBinding
    private lateinit var adapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentEventsBinding.inflate(layoutInflater)

        adapter = EventsAdapter(object : OnInteractionListener {
            override fun onEdit(event: DataItem) {
                val direction =
                    if (requireParentFragment() is FeedFragment)
                        FeedFragmentDirections.actionFeedFragmentToNewPostFragment(editingData = event as EventListItem)
                    else
                        UserPageFragmentDirections.actionUserPageFragmentToNewPostFragment(
                            editingData = event as EventListItem
                        )
                findNavController().navigate(direction)
            }

            override fun onLike(event: DataItem) {
                if (!authViewModel.authorized)
                    showAuthorizationQuestionDialog()
                else {
                    viewModel.likeById(event.id, event.likedByMe)
                }
            }

            override fun onLikeLongClick(view: View, event: DataItem) {
                val popupMenu = AdditionalFunctions.prepareUsersPopupMenu(
                    requireContext(),
                    view,
                    event.likeOwnerIds,
                    event.users,
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

            override fun onRemove(event: DataItem) {
                viewModel.removeById(event.id)
            }

            override fun onCoordinatesClick(coordinates: Coordinates) {
                showMap(coordinates)
            }

            override fun onAvatarClick(authorId: Long) {
                //postViewModel.setFilterBy(authorId)
                filters.setFilterBy(authorId)
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel.clearFeedModelState()

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
            //if (it != null) authViewModel.getUserById(it.id) else setActionBarSubTitle()
            adapter.refresh()
        }

        authViewModel.authUser.observe(viewLifecycleOwner) {
            authUser = it
            //setActionBarSubTitle(it?.name)
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
            //postViewModel.setFilterBy(it.itemId.toLong())
            filters.setFilterBy(it.itemId.toLong())
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

//    private fun setActionBarSubTitle(subTitle: String? = null) {
//        val actionBar = (activity as AppCompatActivity).supportActionBar
//        actionBar?.subtitle = subTitle ?: ""
//    }

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