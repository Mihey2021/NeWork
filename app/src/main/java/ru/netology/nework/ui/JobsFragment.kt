package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.JobsAdapter
import ru.netology.nework.adapters.JobsListActionListener
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.jobs.Job
import ru.netology.nework.utils.AdditionalFunctions.Companion.showErrorDialog
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.JobsViewModel
import ru.netology.nework.viewmodels.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class JobsFragment : Fragment(R.layout.fragment_jobs) {

    private val viewModel: JobsViewModel by activityViewModels()
    private val postViewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private var dialog: AlertDialog? = null
    lateinit var adapter: JobsAdapter
    private var filterBy: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentJobsBinding.inflate(inflater, container, false)

        postViewModel.filterBy.observe(viewLifecycleOwner) {
            filterBy = it
            setFabAddButtonVisibility(binding.fabJobAdd)
            adapter.showEditingMenu = isMyPage()
            refresh()
        }

        adapter = JobsAdapter(object : JobsListActionListener {
            override fun onRemove(id: Long) {
                viewModel.removeMyJobById(id)
            }

            override fun onEdit(job: Job) {
                val direction =
                    FeedFragmentDirections.actionFeedFragmentToNewJobFragment(job = job)
                findNavController().navigate(direction)
            }
        }, isMyPage())
        binding.jobsList.adapter = adapter

        viewModel.jobsData.observe(viewLifecycleOwner) { jobList ->
            adapter.submitList(jobList)
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.loading
            if (state.needRefresh) {
                refresh()
            }
            if (state.error) {
                if (dialog?.isShowing == false || dialog == null) showErrorDialog(
                    requireContext(),
                    state.errorMessage
                )
            }
        }

        authViewModel.authData.observe(viewLifecycleOwner) {
            setFabAddButtonVisibility(binding.fabJobAdd)
            adapter.showEditingMenu = isMyPage()
            refresh()
        }

        binding.swiperefresh.setOnRefreshListener {
            refresh()
        }


        binding.fabJobAdd.setOnClickListener {
            if (!viewModel.authorized) {
                showAuthorizationQuestionDialog()
            } else {
                val direction =
                    FeedFragmentDirections.actionFeedFragmentToNewJobFragment()
                findNavController().navigate(direction)
            }
        }

        return binding.root
    }

    private fun setFabAddButtonVisibility(view: View) {
        view.isVisible = isMyPage()
    }

    private fun refresh() {
        if (filterBy != 0L) {
            if (isMyPage())
                viewModel.getMyJobs()
            else
                viewModel.getUserJobs(filterBy)
        }
    }

    private fun isMyPage(): Boolean {
        val authorizedUserId = appAuth.getAuthorizedUserId()
        return ((authorizedUserId != 0L) && (filterBy == authorizedUserId))
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