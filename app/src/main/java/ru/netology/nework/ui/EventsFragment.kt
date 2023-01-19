package ru.netology.nework.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.EventsAdapter
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.adapters.DataLoadingStateAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.Coordinates
import ru.netology.nework.models.DataItem
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.user.User
import ru.netology.nework.models.user.UserPreview
import ru.netology.nework.viewmodels.EventViewModel

@AndroidEntryPoint
class EventsFragment : Fragment(R.layout.fragment_events) {

    private val viewModel: EventViewModel by activityViewModels()

    private var dialog: AlertDialog? = null
    private var authUser: User? = null
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEventsBinding.inflate(inflater)


        adapter = EventsAdapter(object : OnInteractionListener {
            override fun onEdit(event: DataItem) {
                val direction = FeedFragmentDirections.actionFeedFragmentToNewPostFragment(editingData = event as EventListItem)
                findNavController().navigate(direction)
            }

            override fun onLike(event: DataItem) {
                if (!viewModel.authorized)
                    showAuthorizationQuestionDialog()
                else {
                    viewModel.likeById(event.id, event.likedByMe)
                }
            }

            override fun onLikeLongClick(view: View, event: DataItem) {
                //Toast.makeText(requireContext(), userIds.toString(), Toast.LENGTH_LONG).show()
                showUsersPopupMenu(view, event.likeOwnerIds, event.users)
            }

            override fun onRemove(event: DataItem) {
                viewModel.removeById(event.id)
            }

            override fun onCoordinatesClick(coordinates: Coordinates) {
                showMap(coordinates)
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

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.needRefresh) adapter.refresh()
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

        viewModel.authData.observe(viewLifecycleOwner) {
            if (it != null) viewModel.getUserById(it.id.toLong()) else setActionBarSubTitle()
            adapter.refresh()
        }

        viewModel.authUser.observe(viewLifecycleOwner) {
            authUser = it
            setActionBarSubTitle(it?.name)
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }


        binding.fabEventAdd.setOnClickListener {
            if (!viewModel.authorized) {
                showAuthorizationQuestionDialog()
            }
            else {
                //findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
                val direction = FeedFragmentDirections.actionFeedFragmentToNewPostFragment(isNewEvent = true)
                findNavController().navigate(direction)
            }
        }

        return binding.root
    }

    private fun showMap(coordinates: Coordinates) {
        val direction = FeedFragmentDirections.actionFeedFragmentToMapFragment(coordinates = coordinates, readOnly = true)
        findNavController().navigate(direction)
    }

    private fun showUsersPopupMenu(view: View, usersList: List<Long>, users: Map<Long, UserPreview>) {
        val popupMenu = PopupMenu(view.context, view)
        usersList.forEach { userId ->
            popupMenu.menu.add(
                0,
                userId.toInt(),
                Menu.NONE,
                if (authUser?.id == userId) getString(R.string.me_text) else users[userId]?.name
                    ?: getString(R.string.undefined)
            )
        }

        popupMenu.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
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