package ru.netology.nework.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentAuthorizationBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.utils.AndroidUtils
import ru.netology.nework.viewmodels.AuthorizationViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AuthorizationFragment : Fragment() {

    private val viewModel: AuthorizationViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private var dialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentAuthorizationBinding.inflate(inflater, container, false)

        with(binding) {
            signInButton.setOnClickListener {
                if (checkForm(binding)) {
                    AndroidUtils.hideKeyboard(requireView())
                    //Авторизация на сервере
                    viewModel.authorization(
                        binding.loginEditText.text.toString(),
                        binding.passwordEditText.text.toString()
                    )
                }
            }

            loginEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    loginTextInputLayout.error = null
                }

                override fun afterTextChanged(p0: Editable?) {}
            })

            passwordEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    passwordTextInputLayout.error = null
                }

                override fun afterTextChanged(p0: Editable?) {}
            })

        }

        viewModel.authorizationData.observe(viewLifecycleOwner) { token ->
            if (token == null) return@observe

            appAuth.setAuth(token.id, token.token ?: "")

            findNavController().navigateUp()
        }

        viewModel.dataState.observe(viewLifecycleOwner) {

            dialog?.dismiss()

            if (it.loading) {
                dialog = AppDialogs.getDialog(
                    requireContext(),
                    AppDialogs.PROGRESS_DIALOG,
                    title = getString(R.string.authorization)
                )
            }

            if (it.error) {
                dialog = AppDialogs.getDialog(
                    requireContext(),
                    AppDialogs.ERROR_DIALOG,
                    title = getString(R.string.authorization),
                    message = it.errorMessage ?: getString(R.string.an_error_has_occurred),
                    titleIcon = R.drawable.ic_baseline_error_24,
                    isCancelable = true
                )
            }
        }

        return binding.root
    }

    private fun checkForm(binding: FragmentAuthorizationBinding): Boolean {
        var isCorrect = true

        if (binding.loginEditText.text.isNullOrBlank()) {
            binding.loginTextInputLayout.error = getString(R.string.enter_your_username)
            isCorrect = false
        }

        if (binding.passwordEditText.text.isNullOrBlank()) {
            binding.passwordTextInputLayout.error = getString(R.string.enter_the_password)
            isCorrect = false
        }

        return isCorrect
    }
}