package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.ArrayWithImageAdapter
import ru.netology.nework.databinding.FragmentUserListBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.models.user.User
import ru.netology.nework.viewmodels.CommonViewModel

@AndroidEntryPoint
class UserListFragment: Fragment(R.layout.fragment_user_list) {

    private val viewModel: CommonViewModel by activityViewModels()

    private var dialog: AlertDialog? = null

    private var usersArray: Array<User> = emptyArray()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentUserListBinding.inflate(inflater, container, false)

        viewModel.getAllUsersList()

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                if (dialog?.isShowing == false || dialog == null) showErrorDialog(state.errorMessage)
            }
        }

        viewModel.usersList.observe(viewLifecycleOwner) {usersList ->
            usersArray = usersList.toTypedArray()
            binding.userListView.adapter = ArrayWithImageAdapter(requireContext(), R.layout.user_item, usersArray)
        }

        binding.userListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
//                val dataItem:  = usersArray!![position] as DataModel
//                dataItem.checked = !dataItem.checked
//                adapter.notifyDataSetChanged()
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
}