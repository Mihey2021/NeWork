package ru.netology.nework.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapters.ArrayWithImageAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.*
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
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post) {

    private val postViewModel: PostViewModel by activityViewModels()
    private val eventViewModel: EventViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private lateinit var fragmentBinding: FragmentNewPostBinding

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
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )
        fragmentBinding = binding

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

        binding.coordinatesText.setOnClickListener {
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
                        if (validateForm()) {
                            fragmentBinding.let {
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


            if (data is PostListItem) {
                mentionIds = checkedUsers.users.keys.toList()
            } else {
                speakerIds = checkedUsers.users.keys.toList()
            }

            updateMentionUsersText(checkedUsers)
        }

        return binding.root
    }

    private fun validateForm(): Boolean {
        var valid = !fragmentBinding.content.text.isNullOrBlank()

        if (!valid) {
            showErrorDialog(
                requireContext(),
                "${getString(R.string.error_in_form_data)}\n${getString(R.string.make_sure_required_fields_filled)}"
            )
        }

        return valid
    }

    private fun updateMentionUsersText(checkedUsers: UsersSelected) {
        if (data is PostListItem) {
            fragmentBinding.mentionUsersText.setText(getStringUserList(checkedUsers.users))
        } else {
            fragmentBinding.speakersText.setText(getStringUserList(checkedUsers.users))

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
    private fun saveInEventViewModel(viewModel: EventViewModel, content: String, link: String?) {
        val date = AdditionalFunctions.getFormattedStringDateTime(
            fragmentBinding.eventDateText.text.toString(),
            "dd.MM.yyyy",
            "yyyy-MM-dd"
        )
        val time = "${fragmentBinding.eventTimeText.text.toString()}:01"
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
        fragmentBinding.coordinatesText.setText(
            coordinates?.toString() ?: ""
        )
    }

    private fun openMapFragment() {
        val direction =
            NewPostFragmentDirections.actionNewPostFragmentToMapFragment(coordinates = coordinates)
        findNavController().navigate(direction)
    }

    private fun initUi(data: DataItem?) {
        fragmentBinding.contentLayout.hint = setFieldRequiredHint(fragmentBinding.contentLayout)
        if (data is EventListItem || isNewEvent) {
            with(fragmentBinding) {
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

                if (data != null) {
                    eventDateText.isEnabled = false
                    eventTimeText.isEnabled = false
                } else {
                    eventDateLayout.setEndIconOnClickListener {
                        showDatePicker()
                    }
                    eventTimeLayout.setEndIconOnClickListener {
                        showTimePicker()
                    }
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
            with(fragmentBinding) {
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
        val attachment = data.attachment
        updateCoordinatesText(data.coords)
        with(fragmentBinding) {
            content.setText(data.content)
            linkText.setText(data.link)
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
            fragmentBinding.eventTimeText.setText(
                getFormattedStringTime(
                    timePicker.hour,
                    timePicker.minute
                )
            )
        }
    }

    private fun getFormattedStringTime(hour: Int, minute: Int, separator: String = ":"): String {
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
            fragmentBinding.eventDateText.setText(
                AdditionalFunctions.getFormattedDateTimeToString(
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
}