package ru.netology.nework.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.jobs.Job
import ru.netology.nework.utils.AdditionalFunctions.Companion.convertStringDateToLong
import ru.netology.nework.utils.AdditionalFunctions.Companion.getCurrentDateTime
import ru.netology.nework.utils.AdditionalFunctions.Companion.getFormattedDateTimeToString
import ru.netology.nework.utils.AdditionalFunctions.Companion.getFormattedStringDateTime
import ru.netology.nework.utils.AdditionalFunctions.Companion.setFieldRequiredHint
import ru.netology.nework.utils.AdditionalFunctions.Companion.showErrorDialog
import ru.netology.nework.viewmodels.JobsViewModel
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class NewJobFragment : Fragment(R.layout.fragment_new_job) {

    private val viewModel: JobsViewModel by activityViewModels()
    val args: NewJobFragmentArgs by navArgs()

    private var editJob: Job? = null

    @Inject
    lateinit var appAuth: AppAuth

    lateinit var binding: FragmentNewJobBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewJobBinding.inflate(inflater, container, false)

        editJob = args.job

        init(editJob)


        binding.startDateLayout.setStartIconOnClickListener {
            showDatePicker(binding.startDate, getString(R.string.start_date))
        }
        binding.startDateLayout.setEndIconOnClickListener {
            binding.startDate.text?.clear()
        }
        binding.endDateLayout.setStartIconOnClickListener {
            showDatePicker(
                binding.endDate,
                getString(R.string.end_date),
                convertStringDateToLong(
                    binding.startDate.text.toString(),
                    "dd.MM.yyyy"
                )
            )
        }
        binding.endDateLayout.setEndIconOnClickListener {
            binding.endDate.text?.clear()
        }

        return binding.root
    }

    private fun init(editJob: Job?) {
        with(binding) {
            nameLayout.hint = setFieldRequiredHint(nameLayout)
            positionLayout.hint = setFieldRequiredHint(positionLayout)
            startDateLayout.hint = setFieldRequiredHint(startDateLayout)
            if (editJob != null) {
                name.setText(editJob.name)
                position.setText(editJob.position)
                startDate.setText(getFormattedSateString(editJob.start))
                endDate.setText(getFormattedSateString(editJob.finish ?: ""))
                link.setText(editJob.link)
            } else {
                startDate.setText(getFormattedDateTimeToString(getCurrentDateTime()))
            }
        }
        createMainMenu()
    }

    private fun getFormattedSateString(dateString: String): String =
        getFormattedStringDateTime(
            stringDateTime = dateString,
            patternTo = "dd.MM.yyyy",
        ).toString()


    private fun createMainMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.editing_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        if (validateForm()) {
                            with(binding) {
                                val formattedStringStartDate =
                                    getFormattedStringDateTime(
                                        stringDateTime = "${startDate.text}",
                                        pattern = "dd.MM.yyyy",
                                        patternTo = "yyyy-MM-dd'T'HH:mm:ss.uuuuuu'Z'"
                                    ).toString()
                                val formattedStringEndDate = if (!endDate.text.isNullOrBlank()) {
                                    getFormattedStringDateTime(
                                        stringDateTime = "${endDate.text}",
                                        pattern = "dd.MM.yyyy",
                                        patternTo = "yyyy-MM-dd'T'HH:mm:ss.uuuuuu'Z'"
                                    ).toString()
                                } else null
                                viewModel.saveMyJob(
                                    job = Job(
                                        id = editJob?.id ?: 0L,
                                        name = name.text.toString(),
                                        position = position.text.toString(),
                                        start = formattedStringStartDate,
                                        finish = formattedStringEndDate,
                                        link = if (link.text.isNullOrBlank()) null else link.text.toString(),
                                    )
                                )
                            }
                            findNavController().navigateUp()
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
    }

    private fun validateForm(): Boolean {
        var valid: Boolean

        with(binding) {
            valid = (
                    !name.text.isNullOrBlank() &&
                            !position.text.isNullOrBlank() &&
                            !startDate.text.isNullOrBlank()
                    )
        }

        if (!valid) {
            showErrorDialog(
                requireContext(),
                "${getString(R.string.error_in_form_data)}\n${getString(R.string.make_sure_required_fields_filled)}"
            )
        }

        return valid
    }

    private fun compareStringDate(startDate: String, endDate: String) =
        (getStringDateAsLong(startDate) < getStringDateAsLong(endDate))

    private fun getStringDateAsLong(stringDate: String): Long =
        convertStringDateToLong(
            stringDate,
            "dd.MM.yyyy"
        )


    private fun showDatePicker(
        view: TextInputEditText,
        title: String,
        constraintDate: Long? = null
    ) {
        val constraintsBuilder = CalendarConstraints.Builder()
        if (constraintDate != null) {
            val dateValidatorMin: CalendarConstraints.DateValidator =
                DateValidatorPointForward.from(constraintDate)
            val listValidators = ArrayList<CalendarConstraints.DateValidator>()
            listValidators.add(dateValidatorMin)
            val validators = CompositeDateValidator.allOf(listValidators)
            constraintsBuilder.setValidator(validators)
        }

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

        datePicker.addOnPositiveButtonClickListener {
            val isStartDateField = (view.id == R.id.startDate)
            var isError = false
            val date = Date(it)
            val dateToString = getFormattedDateTimeToString(
                date,
                "dd.MM.yyyy"
            )
            if (isStartDateField && !binding.endDate.text.isNullOrBlank()) {
                if (!compareStringDate(
                        startDate = dateToString,
                        endDate = binding.endDate.text.toString()
                    )
                ) {
                    isError = true
                }
            }
            if (!isError)
                view.setText(dateToString)
            else
                showErrorDialog(requireContext(), getString(R.string.err_dates_compare))
        }
        datePicker.show(parentFragmentManager, "job_date")
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