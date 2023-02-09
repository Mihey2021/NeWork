package ru.netology.nework.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentRegistrationBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.utils.AndroidUtils
import ru.netology.nework.viewmodels.RegistrationViewModel
import javax.inject.Inject

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val viewModel by viewModels<RegistrationViewModel>()

    @Inject
    lateinit var appAuth: AppAuth

    private var dialog: AlertDialog? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding =  FragmentRegistrationBinding.inflate(inflater, container, false)

        with(binding) {
            val pickPhotoLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    when (it.resultCode) {
                        ImagePicker.RESULT_ERROR -> {
                            Snackbar.make(
                                binding.root,
                                ImagePicker.getError(it.data),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        Activity.RESULT_OK -> {
                            val uri: Uri? = it.data?.data
                            viewModel.changePhoto(uri, uri?.toFile())
                        }
                    }
                }

            binding.takePhotoTextView.setOnClickListener {
                ImagePicker.with(requireActivity())
                    .crop()
                    .compress(2048)
                    .provider(ImageProvider.CAMERA)
                    .createIntent(pickPhotoLauncher::launch)
            }

            binding.choosePhotoTextView.setOnClickListener {
                ImagePicker.with(requireActivity())
                    .crop()
                    .compress(2048)
                    .provider(ImageProvider.GALLERY)
                    .galleryMimeTypes(
                        arrayOf(
                            "image/png",
                            "image/jpeg",
                        )
                    )
                    .createIntent(pickPhotoLauncher::launch)
            }

            registerButton.setOnClickListener {
                if(checkForm(binding)){
                    AndroidUtils.hideKeyboard(requireView())
                    //Регистрация на сервере
                    viewModel.registration(
                        binding.loginEditText.text.toString(),
                        binding.passwordEditText.text.toString(),
                        binding.nameEditText.text.toString()
                    )
                }
            }
        }

        binding.nameEditText.setOnTouchListener { _, _ ->
            binding.nameTextInputLayout.error = null
            false
        }
        binding.loginEditText.setOnTouchListener { _, _ ->
            binding.loginTextInputLayout.error = null
            false
        }
        binding.passwordEditText.setOnTouchListener { _, _ ->
            binding.passwordTextInputLayout.error = null
            false
        }
        binding.confirmationPasswordEditText.setOnTouchListener { _, _ ->
            binding.confirmationPasswordTextInputLayout.error = null
            false
        }

        viewModel.photo.observe(viewLifecycleOwner) {

            if (it.uri == null) return@observe

            binding.profileImage.setImageURI(it.uri)
        }

        viewModel.registrationData.observe(viewLifecycleOwner) { token ->
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
                    title = getString(R.string.registration_text)
                )
            }

            if (it.error) {
                dialog = AppDialogs.getDialog(
                    requireContext(),
                    AppDialogs.ERROR_DIALOG,
                    title = getString(R.string.registration_text),
                    message = it.errorMessage ?: getString(R.string.an_error_has_occurred),
                    titleIcon = R.drawable.ic_baseline_error_24,
                    isCancelable = true
                )
            }
        }

        return binding.root
    }

    private fun checkForm(binding: FragmentRegistrationBinding): Boolean {
        var isCorrect = true

        if (binding.nameEditText.text.isNullOrBlank()) {
            binding.nameTextInputLayout.error = getString(R.string.enter_yor_name)
            isCorrect = false
        }

        if (binding.loginEditText.text.isNullOrBlank()) {
            binding.loginTextInputLayout.error = getString(R.string.enter_your_username)
            isCorrect = false
        }

        if (binding.passwordEditText.text.isNullOrBlank()) {
            binding.passwordTextInputLayout.error = getString(R.string.enter_the_password)
            isCorrect = false
        }

        if (binding.confirmationPasswordEditText.text.isNullOrBlank()) {
            binding.confirmationPasswordTextInputLayout.error = getString(R.string.confirm_your_password)
            isCorrect = false
        }

        if (binding.passwordEditText.text.toString() != binding.confirmationPasswordEditText.text.toString()) {
            binding.confirmationPasswordTextInputLayout.error = getString(R.string.passwords_dont_match)
            isCorrect = false
        }

        return isCorrect
    }
}