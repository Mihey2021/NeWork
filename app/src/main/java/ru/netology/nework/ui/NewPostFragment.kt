package ru.netology.nework.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import ru.netology.nework.R
import ru.netology.nework.adapters.ArrayWithImageAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.*
import ru.netology.nework.models.mediaPlayers.CustomMediaPlayer
import ru.netology.nework.models.mediaPlayers.NewMediaAttachment
import ru.netology.nework.models.event.EventCreateRequest
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.event.EventType
import ru.netology.nework.models.post.PostCreateRequest
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.models.user.UsersSelected
import ru.netology.nework.utils.AdditionalFunctions
import ru.netology.nework.utils.AdditionalFunctions.Companion.getCurrentDateTime
import ru.netology.nework.utils.AdditionalFunctions.Companion.getFormattedDateTimeToString
import ru.netology.nework.utils.AdditionalFunctions.Companion.setFieldRequiredHint
import ru.netology.nework.utils.AdditionalFunctions.Companion.showErrorDialog
import ru.netology.nework.utils.AndroidUtils
import ru.netology.nework.utils.getSerializable
import ru.netology.nework.viewmodels.EventViewModel
import ru.netology.nework.viewmodels.PostViewModel
import java.io.File
import java.util.*
import javax.inject.Inject


const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 111

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post) {

    private val postViewModel: PostViewModel by activityViewModels()
    private val eventViewModel: EventViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    @Inject
    lateinit var customMediaPlayer: CustomMediaPlayer

    private lateinit var binding: FragmentNewPostBinding

    private var dialog: AlertDialog? = null

    private var data: DataItem? = null

    private var coordinates: Coordinates? = null

    private var mentionIds: List<Long> = listOf()
    private var speakerIds: List<Long> = listOf()
    private var participantsIds: List<Long> = listOf()

    private val args: NewPostFragmentArgs by navArgs()
    private var isNewPost: Boolean = false
    private var isNewEvent: Boolean = false

    private var selectedEventType: EventType = EventType.OFFLINE

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        data = args.editingData
        isNewPost = args.isNewPost
        isNewEvent = args.isNewEvent

        if (data != null) {
            coordinates = data!!.coords
            mentionIds = data!!.mentionIds
            speakerIds = data!!.speakerIds
            participantsIds = data!!.participantsIds
        }

        if (data is EventListItem) selectedEventType = data!!.type

        initUi(data)

        setActionBarTitle(data != null)
        binding.content.requestFocus()

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
                        clearDataAttachment()
                        val uri: Uri = it.data?.data ?: return@registerForActivityResult
                        val file = uri.toFile()
                        val mediaType = "image/${
                            MimeTypeMap.getFileExtensionFromUrl(
                                Uri.fromFile(file).toString()
                            )
                        }".toMediaTypeOrNull()

                        if (data is EventListItem || isNewEvent) {
                            eventViewModel.changeMedia(
                                uri,
                                file,
                                AttachmentType.IMAGE,
                                mediaType
                            )
                        } else {
                            postViewModel.changeMedia(
                                uri,
                                file,
                                AttachmentType.IMAGE,
                                mediaType
                            )
                        }
                    }
                }
                setAttachmentVisibility(AttachmentType.IMAGE)
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
            clearDataAttachment()
        }
        binding.removeAudio.setOnClickListener {
            clearDataAttachment()
        }
        binding.removeVideo.setOnClickListener {
            clearDataAttachment()
        }

        val pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    when (it.resultCode) {
                        Activity.RESULT_CANCELED -> {
                            return@registerForActivityResult
                        }
                        Activity.RESULT_OK -> {
                            clearDataAttachment()
                            val uri: Uri = it.data?.data ?: return@registerForActivityResult
                            val mediaType =
                                requireContext().contentResolver.getType(uri)?.toMediaTypeOrNull()
                            if (data is EventListItem || isNewEvent) {
                                eventViewModel.changeMedia(
                                    uri,
                                    getFile(uri),
                                    AttachmentType.VIDEO,
                                    mediaType
                                )
                            } else {
                                postViewModel.changeMedia(
                                    uri,
                                    getFile(uri),
                                    AttachmentType.VIDEO,
                                    mediaType
                                )
                            }
                        }
                    }
                    setAttachmentVisibility(AttachmentType.VIDEO)
                } catch (e: Exception) {
                    showStateDialogs(FeedModelState(errorMessage = e.message))
                }
            }

        binding.pickVideo.setOnClickListener {
            if (!checkPermissionReadExternalStorage()) return@setOnClickListener
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            pickVideoLauncher.launch(
                Intent.createChooser(
                    intent,
                    requireContext().getString(R.string.pick_video)
                )
            )
        }

        val pickAudioLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    when (it.resultCode) {
                        Activity.RESULT_CANCELED -> {
                            return@registerForActivityResult
                        }
                        Activity.RESULT_OK -> {
                            clearDataAttachment()
                            val uri: Uri = it.data?.data ?: return@registerForActivityResult
                            val mediaType =
                                requireContext().contentResolver.getType(uri)?.toMediaTypeOrNull()
                            val file = getFile(uri, isVideo = false)
                            if (data is EventListItem || isNewEvent) {
                                eventViewModel.changeMedia(
                                    uri,
                                    file,
                                    AttachmentType.AUDIO,
                                    mediaType
                                )
                            } else {
                                postViewModel.changeMedia(
                                    uri,
                                    file,
                                    AttachmentType.AUDIO,
                                    mediaType
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    showStateDialogs(FeedModelState(errorMessage = e.message))
                }
            }

        binding.pickAudio.setOnClickListener {
            if (!checkPermissionReadExternalStorage()) return@setOnClickListener
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
            pickAudioLauncher.launch(
                Intent.createChooser(
                    intent,
                    requireContext().getString(R.string.pick_audio)
                )
            )
        }

        binding.coordinatesText.setOnClickListener {
            openMapFragment()
        }

        if (data is PostListItem || isNewPost) {
            postViewModel.dataState.observe(viewLifecycleOwner) {
                showStateDialogs(it)
            }
            postViewModel.postCreated.observe(viewLifecycleOwner) {
                findNavController().navigateUp()
            }
        }

        if (data is EventListItem || isNewEvent) {
            eventViewModel.dataState.observe(viewLifecycleOwner) {
                showStateDialogs(it)
            }
            eventViewModel.eventCreated.observe(viewLifecycleOwner) {
                findNavController().navigateUp()
            }
        }

        if (data is PostListItem || isNewPost) {
            postViewModel.media.observe(viewLifecycleOwner) {
                if (it.uri == null && data?.attachment == null) {
                    setAttachmentVisibility()
                    return@observe
                }
                when (val attachmentType = it?.fileDescription?.second ?: data?.attachment?.type) {
                    AttachmentType.IMAGE -> {
                        setAttachmentVisibility(attachmentType = attachmentType, uri = it.uri)
                    }
                    AttachmentType.AUDIO -> {
                        setAttachmentVisibility(
                            attachmentType = attachmentType,
                            path = it.fileDescription?.first?.absolutePath
                        )
                    }
                    else -> {
                        setAttachmentVisibility(
                            attachmentType = attachmentType,
                            path = it.fileDescription?.first?.absolutePath
                        )
                    }
                }
            }
        }

        if (data is EventListItem || isNewEvent) {
            eventViewModel.media.observe(viewLifecycleOwner) {
                if ((it.uri == null && data?.attachment == null) && data?.attachment?.type == AttachmentType.IMAGE) {
                    setAttachmentVisibility()
                    return@observe
                }
                when (val attachmentType = it?.fileDescription?.second ?: data?.attachment?.type) {
                    AttachmentType.IMAGE -> {
                        setAttachmentVisibility(attachmentType = attachmentType, uri = it.uri)
                    }
                    AttachmentType.AUDIO -> {
                        setAttachmentVisibility(
                            attachmentType = attachmentType,
                            path = it.fileDescription?.first?.absolutePath
                        )
                    }
                    else -> {
                        setAttachmentVisibility(
                            attachmentType = attachmentType,
                            path = it.fileDescription?.first?.absolutePath
                        )
                    }
                }
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.editing_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        if (validateForm()) {
                            binding.let {
                                if (data is PostListItem || isNewPost) {
                                    saveInPostViewModel(
                                        viewModel = postViewModel,
                                        content = it.content.text.toString(),
                                        link = it.linkText.text.toString().trim().ifBlank { null }
                                    )
                                } else {
                                    saveInEventViewModel(
                                        viewModel = eventViewModel,
                                        content = it.content.text.toString(),
                                        link = it.linkText.text.toString().trim().ifBlank { null }
                                    )
                                }
                                AndroidUtils.hideKeyboard(requireView())
                            }
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

            updateCoordinatesText(coordinates)
        }

        parentFragmentManager.setFragmentResultListener(
            UserListFragment.REQUEST_CODE,
            viewLifecycleOwner
        ) { _, resultData ->
            val checkedUsers = getSerializable(
                resultData,
                UserListFragment.EXTRA_SELECTED_USERS_IDS,
                UsersSelected::class.java
            )


            if (data is PostListItem || isNewPost) {
                mentionIds = checkedUsers.users.keys.toList()
            } else {
                speakerIds = checkedUsers.users.keys.toList()
            }

            updateMentionUsersText(checkedUsers)
        }

        return binding.root
    }

    private fun checkPermissionReadExternalStorage(): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    showPermissionReadExternalStorageDialog(
                        getString(R.string.external_storage),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } else {
                    ActivityCompat
                        .requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                        )
                }
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun showPermissionReadExternalStorageDialog(msg: String, permission: String) {
        AppDialogs.getDialog(requireContext(), AppDialogs.QUESTION_DIALOG,
            title = getString(R.string.permission_necessary),
            message = "${getString(R.string.permission_necessary)}: $msg",
            positiveButtonTitle = getString(R.string.request),
            onDialogsInteractionListener = object : OnDialogsInteractionListener {
                override fun onPositiveClickButton() {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(permission),
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                    )
                }
            })
    }

    private fun clearDataAttachment() {

        binding.photoContainer.visibility = View.GONE
        binding.audioPlayerContainer.visibility = View.GONE
        binding.videoPlayerContainer.visibility = View.GONE

        if (data == null) {
            if (isNewPost)
                postViewModel.changeMedia(null, null)
            else
                eventViewModel.changeMedia(null, null)
            return
        }

        data = if (data is PostListItem) {
            val currentData = data as PostListItem
            currentData.copy(post = currentData.post.copy(attachment = null))
        } else {
            val currentData = data as EventListItem
            currentData.copy(event = currentData.event.copy(attachment = null))
        }
    }

    private fun setAttachmentVisibility(
        attachmentType: AttachmentType? = null,
        path: String? = null,
        uri: Uri? = null
    ) {

        if (attachmentType == null) {
            binding.photoContainer.visibility = View.GONE
            binding.audioPlayerContainer.visibility = View.GONE
            binding.videoPlayerContainer.visibility = View.GONE
            return
        }

        val dataAttachment = data?.attachment

        when (attachmentType) {
            AttachmentType.IMAGE -> {
                binding.photoContainer.visibility = View.VISIBLE
                binding.audioPlayerContainer.visibility = View.GONE
                binding.videoPlayerContainer.visibility = View.GONE
                if (dataAttachment != null)
                    loadImage(binding.photo, dataAttachment.url)
                else
                    loadImage(binding.photo, uri)
            }
            AttachmentType.AUDIO -> {
                binding.audioPlayerContainer.visibility = View.VISIBLE
                binding.photoContainer.visibility = View.GONE
                binding.videoPlayerContainer.visibility = View.GONE
                binding.audioPlayerInclude.playStop.setOnClickListener {
                    playStopMediaPlayer(path)
                }
            }
            else -> {
                binding.videoPlayerContainer.visibility = View.VISIBLE
                binding.photoContainer.visibility = View.GONE
                binding.audioPlayerContainer.visibility = View.GONE
                binding.videoPlayerInclude.fullScreen.visibility = View.GONE
                binding.videoPlayerInclude.playStop.setOnClickListener {
                    playStopMediaPlayer(path, isVideo = true)
                }
            }
        }
    }

    private fun showStateDialogs(feedModel: FeedModelState) {
        dialog?.dismiss()

        if (feedModel.loading) {
            dialog = AppDialogs.getDialog(
                requireContext(),
                AppDialogs.PROGRESS_DIALOG,
                title = getString(R.string.save)
            )
        }

        if (feedModel.error) {
            dialog = AppDialogs.getDialog(
                requireContext(),
                AppDialogs.ERROR_DIALOG,
                title = getString(R.string.an_error_has_occurred),
                message = feedModel.errorMessage ?: getString(R.string.an_error_has_occurred),
                titleIcon = R.drawable.ic_baseline_error_24,
                isCancelable = true
            )
        }
    }

    private fun getFile(uri: Uri?, isVideo: Boolean = true): File? {

        val projection = if (isVideo)
            arrayOf(MediaStore.Video.Media.DATA)
        else
            arrayOf(MediaStore.Audio.Media.DATA)

        val cursor = requireContext().contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val idColumn = cursor
                .getColumnIndex(MediaStore.Video.Media.DATA)

            val path = cursor.getString(idColumn)
            cursor.close()
            File(path)

        } else
            null
    }

    private fun validateForm(): Boolean {
        val valid = !binding.content.text.isNullOrBlank()

        if (!valid) {
            showErrorDialog(
                requireContext(),
                "${getString(R.string.error_in_form_data)}\n${getString(R.string.make_sure_required_fields_filled)}"
            )
        }

        return valid
    }

    private fun updateMentionUsersText(checkedUsers: UsersSelected) {
        if (data is PostListItem || isNewPost) {
            binding.mentionUsersText.setText(getStringUserList(checkedUsers.users))
        } else {
            binding.speakersText.setText(getStringUserList(checkedUsers.users))

        }
    }

    private fun getStringUserList(checkedUsers: MutableMap<Long, String>) =
        if (checkedUsers.isEmpty()) "" else checkedUsers.values.toString()

    private fun getListUsersText(usersIds: List<Long>): String {
        var usersNames = ""
        if (data == null || usersIds.isEmpty()) return usersNames

        val authorizedUserId = appAuth.getAuthorizedUserId()
        usersIds.forEach { userId ->
            usersNames += "${
                if (authorizedUserId == userId)
                    getString(R.string.me_text)
                else
                    (data!!.users[userId]?.name ?: "")
            }, "
        }
        return "[${usersNames.substring(0, usersNames.length - 2)}]"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    private fun saveInEventViewModel(
        viewModel: EventViewModel,
        content: String,
        link: String?
    ) {
        val date = AdditionalFunctions.getFormattedStringDateTime(
            binding.eventDateText.text.toString(),
            "dd.MM.yyyy",
            "yyyy-MM-dd"
        )
        val time = "${binding.eventTimeText.text.toString()}:01"
        val eventDate = AdditionalFunctions.getFormattedStringDateTime(
            stringDateTime = "$date $time",
            pattern = "yyyy-MM-dd HH:mm:ss",
            patternTo = "yyyy-MM-dd'T'HH:mm:ss.uuuuuu'Z'"
        ).toString()
        viewModel.edit(
            EventCreateRequest(
                id = if (data == null) 0L else data!!.id,
                content = content,
                coords = coordinates,
                type = selectedEventType,
                link = link,
                attachment = if (data == null) null else data!!.attachment,
                speakerIds = speakerIds,
                datetime = eventDate,
            )
        )

        viewModel.save()
    }

    private fun saveInPostViewModel(viewModel: PostViewModel, content: String, link: String?) {
        viewModel.edit(
            PostCreateRequest(
                id = if (data == null) 0L else data!!.id,
                content = content,
                coords = coordinates,
                link = link,
                attachment = if (data == null) null else data!!.attachment,
                mentionIds = mentionIds,
            )
        )
        viewModel.save()
    }

    private fun updateCoordinatesText(coordinates: Coordinates?) {
        binding.coordinatesText.setText(
            coordinates?.toString() ?: ""
        )
    }

    private fun openMapFragment() {
        val direction =
            NewPostFragmentDirections.actionNewPostFragmentToMapFragment(coordinates = coordinates)
        findNavController().navigate(direction)
    }

    private fun initUi(data: DataItem?) {
        binding.contentLayout.hint = setFieldRequiredHint(binding.contentLayout)
        setAttachmentVisibility(data?.attachment?.type)
        if (data is EventListItem || isNewEvent) {
            with(binding) {
                eventDateLayout.hint = setFieldRequiredHint(eventDateLayout)
                eventTimeLayout.hint = setFieldRequiredHint(eventTimeLayout)
                eventDateLayout.visibility = View.VISIBLE
                eventTimeLayout.visibility = View.VISIBLE
                eventDateText.setText(
                    if (data == null || isNewEvent)
                        getFormattedDateTimeToString(getCurrentDateTime())
                    else
                        AdditionalFunctions.getFormattedStringDateTime(
                            stringDateTime = data.datetime,
                            patternTo = "dd.MM.yyyy",
                        )
                )

                eventTimeText.setText(
                    if (data == null || isNewEvent)
                        getFormattedDateTimeToString(getCurrentDateTime(), "HH:mm")
                    else
                        AdditionalFunctions.getFormattedStringDateTime(
                            stringDateTime = data.datetime,
                            patternTo = "HH:mm"
                        )
                )

                eventDateLayout.setEndIconOnClickListener {
                    showDatePicker()
                }
                eventTimeLayout.setEndIconOnClickListener {
                    showTimePicker()
                }

                eventTypeLayout.visibility = View.VISIBLE
                val adapter = ArrayWithImageAdapter(
                    requireContext(),
                    R.layout.event_type_item,
                    EventType.values()
                )
                eventTypeTextView.setAdapter(adapter)
                eventTypeTextView.setText(
                    if (isNewEvent) EventType.OFFLINE.toString() else data!!.type.toString(),
                    false
                )
                eventTypeTextView.setOnItemClickListener { _, _, position, _ ->
                    selectedEventType = EventType.values()[position]
                }

                speakersLayout.setEndIconOnClickListener {
                    openUserListFragment(
                        getString(R.string.select_speakers),
                        speakerIds,
                        filteredMe = false
                    )
                }
                speakersText.setText(
                    if (speakerIds.isEmpty()) getString(R.string.speakers) else getListUsersText(
                        speakerIds
                    )
                )
                speakersLayout.visibility = View.VISIBLE

                participantsText.setText(
                    getListUsersText(participantsIds)
                )
                participantsLayout.visibility =
                    if (participantsIds.isEmpty()) View.GONE else View.VISIBLE
                mentionUsersLayout.visibility = View.GONE
            }
        } else {
            with(binding) {
                eventDateLayout.visibility = View.GONE
                eventTimeLayout.visibility = View.GONE
                eventTypeLayout.visibility = View.GONE
                mentionUsersText.setText(
                    if (mentionIds.isEmpty()) getString(R.string.marked_users) else getListUsersText(
                        mentionIds
                    )
                )
                mentionUsersLayout.visibility = View.VISIBLE
                mentionUsersLayout.setEndIconOnClickListener {
                    openUserListFragment(getString(R.string.mark_users), mentionIds)
                }
                speakersLayout.visibility = View.GONE
                participantsLayout.visibility = View.GONE
            }

        }

        if (data == null) {
            updateCoordinatesText(coordinates)
            return
        }
        updateCoordinatesText(data.coords)
        with(binding) {
            content.setText(data.content)
            linkText.setText(data.link)
        }

    }

    private fun openUserListFragment(
        title: String,
        idsList: List<Long>,
        filteredMe: Boolean = true
    ) {
        val direction =
            NewPostFragmentDirections.actionNewPostFragmentToUserListFragment(
                title = title,
                selectedUsersIds = (idsList.toLongArray()),
                filteredMe = filteredMe,
            )
        findNavController().navigate(direction)
    }

    @SuppressLint("SetTextI18n")
    private fun showTimePicker() {
        val isSystem24Hour = DateFormat.is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val dateTime = getCurrentDateTime()
        val hour = AdditionalFunctions.getFormattedDateTimeToInt(dateTime, "HH")
        val minute = AdditionalFunctions.getFormattedDateTimeToInt(dateTime, "mm")

        val timePicker =
            MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(getString(R.string.event_time))
                .build()

        timePicker.show(parentFragmentManager, "event_time")

        timePicker.addOnPositiveButtonClickListener {
            binding.eventTimeText.setText(
                getFormattedStringTime(
                    timePicker.hour,
                    timePicker.minute
                )
            )
        }
    }

    private fun getFormattedStringTime(
        hour: Int,
        minute: Int,
        separator: String = ":"
    ): String {
        val strHour = if (hour < 10) "0$hour" else hour.toString()
        val strMinute = if (minute < 10) "0$minute" else minute.toString()
        return "${strHour}${separator}${strMinute}"
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val dateValidatorMin: CalendarConstraints.DateValidator =
            DateValidatorPointForward.from(today)
        val listValidators = ArrayList<CalendarConstraints.DateValidator>()
        listValidators.add(dateValidatorMin)
        val validators = CompositeDateValidator.allOf(listValidators)
        constraintsBuilder.setValidator(validators)

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.event_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

        datePicker.addOnPositiveButtonClickListener {
            val date = Date(it)
            binding.eventDateText.setText(
                getFormattedDateTimeToString(
                    date,
                    "dd.MM.yyyy"
                )
            )
        }
        datePicker.show(parentFragmentManager, "event_date")
    }

    private fun loadImage(imageView: ImageView, url: String) {
        Glide.with(imageView)
            .load(url)
            .placeholder(R.drawable.ic_baseline_loading_24)
            .error(R.drawable.ic_baseline_non_loaded_image_24)
            .timeout(10_000)
            .into(imageView)
    }

    private fun loadImage(imageView: ImageView, uri: Uri?) {
        if (uri == null) return
        Glide.with(imageView)
            .load(uri)
            .placeholder(R.drawable.ic_baseline_loading_24)
            .error(R.drawable.ic_baseline_non_loaded_image_24)
            .timeout(10_000)
            .into(imageView)
    }

    private fun setActionBarTitle(editing: Boolean) {
        val actionBar = (activity as AppCompatActivity).supportActionBar

        val title = if (data is PostListItem || isNewPost)
            if (editing) getString(R.string.edit_post) else getString(R.string.add_post)
        else
            if (editing) getString(R.string.edit_event) else getString(R.string.add_event)

        actionBar?.title = title
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

    override fun onPause() {
        super.onPause()
        if (data is PostListItem) {
            val post = postViewModel.getMediaPlayingPost()
            if (post != null)
                customMediaPlayer.stopMediaPlaying(PostListItem(post = post) as DataItem)
        } else {
            val event = eventViewModel.getMediaPlayingEvent()
            if (event != null)
                customMediaPlayer.stopMediaPlaying(EventListItem(event = event) as DataItem)
        }
    }

    private fun playStopMediaPlayer(path: String? = null, isVideo: Boolean = false) {
        if (path == null) {
            val currentData = if (data is PostListItem) {
                val postItem = data as PostListItem
                postItem.copy(post = postItem.post.copy(isPlayed = !binding.audioPlayerInclude.playStop.isChecked))
            } else {
                val eventItem = data as EventListItem
                eventItem.copy(event = eventItem.event.copy(isPlayed = !binding.audioPlayerInclude.playStop.isChecked))

            }
            if (isVideo)
                customMediaPlayer.playStopVideo(currentData, binding.videoPlayerInclude)
            else
                customMediaPlayer.playStopAudio(currentData, binding.audioPlayerInclude)
        } else {
            if (isVideo)
                customMediaPlayer.playStopVideo(
                    binding = binding.videoPlayerInclude,
                    newMediaAttachment = NewMediaAttachment(
                        url = path,
                        nowPlaying = !binding.videoPlayerInclude.playStop.isChecked
                    )
                )
            else
                customMediaPlayer.playStopAudio(
                    binding = binding.audioPlayerInclude,
                    newMediaAttachment = NewMediaAttachment(
                        url = path,
                        nowPlaying = !binding.audioPlayerInclude.playStop.isChecked
                    )
                )
        }
    }

    override fun onStop() {
        super.onStop()
        customMediaPlayer.stopMediaPlaying(onlyStop = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (data is PostListItem || isNewPost)
            postViewModel.clearMedia()
        else
            eventViewModel.clearMedia()
    }
}

