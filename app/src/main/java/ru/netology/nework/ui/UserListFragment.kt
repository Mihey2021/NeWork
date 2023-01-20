package ru.netology.nework.ui

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.collection.arrayMapOf
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.ArrayWithImageAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserListBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.user.User
import ru.netology.nework.models.user.UserDataModel
import ru.netology.nework.viewmodels.CommonViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserListFragment : Fragment(R.layout.fragment_user_list) {

    private val viewModel: CommonViewModel by activityViewModels()

    private var dialog: AlertDialog? = null

    private var usersArray: Array<User> = emptyArray()

    lateinit var adapter: ArrayWithImageAdapter<User>

    private val args: UserListFragmentArgs by navArgs()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentUserListBinding.inflate(inflater, container, false)

        viewModel.getAllUsersList()

        val selectedUsersIds = args.selectedUsersIds.toMutableSet()

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                if (dialog?.isShowing == false || dialog == null) showErrorDialog(state.errorMessage)
            }
        }

        viewModel.usersList.observe(viewLifecycleOwner) { usersList ->
            usersList.forEach { user ->
                usersArray += user.copy(isChecked = selectedUsersIds.contains(user.id))

            }
            adapter = ArrayWithImageAdapter(requireContext(), R.layout.user_item, usersArray)
            binding.userListView.adapter = adapter
        }

        binding.userListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
//                val dataItem:  = usersArray!![position] as DataModel
//                dataItem.checked = !dataItem.checked
//                adapter.notifyDataSetChanged()
                val dataItem = usersArray[position]
                val userId = dataItem.id
                val arrayDo = usersArray[position]
                usersArray[position].isChecked = !usersArray[position].isChecked
                val arrayPosle = usersArray[position]
                adapter.notifyDataSetChanged()
                if (selectedUsersIds.contains(userId))
                    selectedUsersIds.remove(userId)
                else
                    selectedUsersIds.add(userId)
            }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.editing_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        val selectedUsers = UserDataModel()
                        usersArray.filter { user -> user.isChecked }.map{user -> selectedUsers.users.put(user.id, user.name)}
                        parentFragmentManager.setFragmentResult(
                            REQUEST_CODE, bundleOf(
                                EXTRA_SELECTED_USERS_IDS to selectedUsers//selectedUsersIds.toLongArray()
                            )
                        )
                        findNavController().navigateUp()
                        true
                    }
                    R.id.logout -> {
                        showLogoutQuestionDialog()
                        true
                    }
                    else -> false
                }
        }, viewLifecycleOwner)

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

    private fun showLogoutQuestionDialog() {
        AppDialogs.getDialog(requireContext(), AppDialogs.QUESTION_DIALOG,
            title = getString(R.string.logout),
            message = getString(R.string.do_you_really_want_to_get_out),
            titleIcon = R.drawable.ic_baseline_logout_24,
            positiveButtonTitle = getString(R.string.yes_text),
            onDialogsInteractionListener = object : OnDialogsInteractionListener {
                override fun onPositiveClickButton() {
                    appAuth.removeAuth()
                    findNavController().popBackStack(R.id.feedFragment, false)
                }
            })
    }

    companion object {
        const val REQUEST_CODE = "REQUEST_CODE"
        const val EXTRA_SELECTED_USERS_IDS = "EXTRA_SELECTED_USERS_IDS"
    }
}