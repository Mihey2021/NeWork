package ru.netology.nework.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.*
import ru.netology.nework.models.event.EventCreateRequest
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.post.PostCreateRequest
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.utils.AndroidUtils
import ru.netology.nework.viewmodels.EventViewModel
import ru.netology.nework.viewmodels.PostViewModel
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post) {

    private val postViewModel: PostViewModel by activityViewModels()
    private val eventViewModel: EventViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private lateinit var fragmentBinding: FragmentNewPostBinding

    //private var post: PostListItem? = null
    private var data: DataItem? = null

    private var coordinates: Coordinates? = null

    private val args: NewPostFragmentArgs by navArgs()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )
        fragmentBinding = binding

        //post = args.editingPost
        data = args.editingData
        val isNewPost = args.isNewPost
        val isNewEvent = args.isNewEvent
//        if(data is PostListItem) {
//            data = data as PostListItem
//            viewModel = postViewModel
//        }
//        if(data is EventListItem) {
//            data = data as EventListItem
//            viewModel = eventViewModel
//        }


//        if(isNewPost)
//            viewModel = postViewModel
//        else
//            viewModel = eventViewModel

        initUi(data)

        setActionBarTitle(data != null)
        binding.edit.requestFocus()

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
                        if (data is EventListItem || isNewEvent) {
                            eventViewModel.changePhoto(uri, uri?.toFile())
                        } else {
                            postViewModel.changePhoto(uri, uri?.toFile())
                        }
                    }
                }
            }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
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

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.CAMERA)
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.removePhoto.setOnClickListener {
            if (data == null) {
                if (isNewPost)
                    postViewModel.changePhoto(null, null)
                else
                    eventViewModel.changePhoto(null, null)
                return@setOnClickListener
            }
            fragmentBinding.photoContainer.visibility = View.GONE
            data = if (data is PostListItem) {
                val currentData = data as PostListItem
                currentData.copy(post = currentData.post.copy(attachment = null))
            } else {
                val currentData = data as EventListItem
                currentData.copy(event = currentData.event.copy(attachment = null))
            }

        }

        binding.coordinates.setOnClickListener {
            openMapFragment()
        }

        if (data is PostListItem || isNewPost) {
            postViewModel.postCreated.observe(viewLifecycleOwner) {
                findNavController().navigateUp()
            }
        }

        if (data is EventListItem || isNewEvent) {
            eventViewModel.eventCreated.observe(viewLifecycleOwner) {
                findNavController().navigateUp()
            }
        }

        if (data is PostListItem || isNewPost) {
            postViewModel.photo.observe(viewLifecycleOwner) {
                if (it.uri == null && data?.attachment == null) {
                    binding.photoContainer.visibility = View.GONE
                    return@observe
                }
                binding.photoContainer.visibility = View.VISIBLE
                binding.photo.setImageURI(it.uri)
            }
        }

        if (data is EventListItem || isNewEvent) {
            eventViewModel.photo.observe(viewLifecycleOwner) {
                if (it.uri == null && data?.attachment == null) {
                    binding.photoContainer.visibility = View.GONE
                    return@observe
                }
                binding.photoContainer.visibility = View.VISIBLE
                binding.photo.setImageURI(it.uri)
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.editing_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        fragmentBinding.let {
                            if (data is PostListItem || isNewPost) {
                                saveInPostViewModel(
                                    viewModel = postViewModel,
                                    content = it.edit.text.toString(),
                                    link = it.linkText.text.toString()
                                )
                            } else {
                                saveInEventViewModel(
                                    viewModel = eventViewModel,
                                    content = it.edit.text.toString(),
                                    link = it.linkText.text.toString()
                                )
                            }
                            AndroidUtils.hideKeyboard(requireView())
                        }
                        true
                    }
                    R.id.logout -> {
                        showLogoutQuestionDialog()
                        true
                    }
                    else -> false
                }

        }, viewLifecycleOwner)

        parentFragmentManager.setFragmentResultListener(
            MapFragment.REQUEST_CODE,
            viewLifecycleOwner
        ) { _, resultData ->
            coordinates =
                getSerializable(resultData, MapFragment.EXTRA_COORDINATES, Coordinates::class.java)
            if (data != null) {
                data = if (data is PostListItem)
                    (data as PostListItem).copy(post = (data as PostListItem).post.copy(coords = coordinates))
                else
                    (data as EventListItem).copy(event = (data as EventListItem).event.copy(coords = coordinates))
            }
            updateCoordinatesText(coordinates)
        }

        return binding.root
    }

    private fun <T : Serializable?> getSerializable(
        data: Bundle,
        name: String,
        clazz: Class<T>
    ): T {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            data.getSerializable(name, clazz)!!
        else
            data.getSerializable(name) as T
    }

    private fun saveInEventViewModel(viewModel: EventViewModel, content: String, link: String) {
        viewModel.edit(
            if (data == null) EventCreateRequest(
                id = 0,
                content = "",
                coords = coordinates
            ) else EventCreateRequest(
                id = data!!.id,
                content = data!!.content,
                coords = data!!.coords,
                link = data!!.link,
                attachment = data!!.attachment,
                speakerIds = data!!.speakerIds
            )
        )
        viewModel.changeContent(content)
        viewModel.changeLink(link)
        viewModel.save()
    }

    private fun saveInPostViewModel(viewModel: PostViewModel, content: String, link: String) {
        viewModel.edit(
            if (data == null) PostCreateRequest(
                id = 0,
                content = "",
                coords = coordinates
            ) else PostCreateRequest(
                id = data!!.id,
                content = data!!.content,
                coords = data!!.coords,
                link = data!!.link,
                attachment = data!!.attachment,
                mentionIds = data!!.mentionIds
            )
        )

        viewModel.changeContent(content)
        viewModel.changeLink(link)
        viewModel.save()
    }

    private fun updateCoordinatesText(coordinates: Coordinates?) {
        if (data != null) {
            fragmentBinding.coordinates.text =
                if (data?.coords == null) getString(R.string.set_location) else data!!.coords.toString()
        } else
            fragmentBinding.coordinates.text =
                coordinates?.toString() ?: getString(R.string.set_location)
    }

    private fun openMapFragment() {
        val direction =
            NewPostFragmentDirections.actionNewPostFragmentToMapFragment(coordinates = data?.coords)
        findNavController().navigate(direction)
    }

    private fun initUi(data: DataItem?) {
        if (data == null) {
            fragmentBinding.eventTypeTextView.setAdapter(ArrayAdapter<EventType>(requireContext(), android.R.layout.simple_list_item_1, EventType.values()))
            updateCoordinatesText(coordinates)
            return
        }
        val attachment = data.attachment
        updateCoordinatesText(data.coords)
        with(fragmentBinding) {
            edit.setText(data.content)
            linkText.setText(data.link)
            eventTypeTextView.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, EventType.values()))
            when (attachment?.type) {
                AttachmentType.IMAGE -> {
                    loadImage(fragmentBinding.photo, attachment.url)
                }
                AttachmentType.VIDEO -> {
                    //TODO()
                }
                AttachmentType.AUDIO -> {
                    //TODO()
                }
                null -> {
                    //TODO()
                }
            }
        }

    }

    private fun loadImage(imageView: ImageView, url: String) {
        Glide.with(imageView)
            .load(url)
            .placeholder(R.drawable.ic_baseline_loading_24)
            .error(R.drawable.ic_baseline_non_loaded_image_24)
            .timeout(10_000)
            .into(imageView)
    }

    private fun setActionBarTitle(editing: Boolean) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title =
            if (editing) getString(R.string.edit_post) else getString(R.string.add_post)
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
                    findNavController().navigateUp()
                }
            })
    }

    override fun onDestroyView() {
        //fragmentBinding = null
        super.onDestroyView()
    }
}