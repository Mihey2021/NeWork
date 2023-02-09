package ru.netology.nework.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.R
import ru.netology.nework.databinding.JobCardBinding
import ru.netology.nework.models.jobs.Job
import ru.netology.nework.utils.AdditionalFunctions

interface JobsListActionListener {
    fun onRemove(id: Long)
    fun onEdit(job: Job)
}

class JobsAdapter(
    private val actionListener: JobsListActionListener,
    var showEditingMenu: Boolean,
) : ListAdapter<Job, JobsAdapter.JobsViewHolder>(JobDiffCallback()) {

    class JobsViewHolder(
        private val binding: JobCardBinding,
        private val actionListener: JobsListActionListener,
        private val showEditingMenu: Boolean,
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(job: Job) {
            binding.apply {
                name.text = job.name
                position.setText(job.position)
                startDate.setText(getFormattedStringData(job.start))
                endDate.setText(getFormattedStringData(job.finish ?: ""))
                link.text = job.link ?: ""
                menu.isVisible = showEditingMenu
                menu.setOnClickListener {
                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.options_post)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.remove -> {
                                    actionListener.onRemove(job.id)
                                    true
                                }
                                R.id.content -> {
                                    actionListener.onEdit(job)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            }
        }

        private fun getFormattedStringData(dateString: String): String =
            AdditionalFunctions.getFormattedStringDateTime(
                stringDateTime = dateString,
                returnOriginalDateIfExceptException = true,
                patternTo = "dd.MM.yyyy"
            ).toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val binding = JobCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobsViewHolder(binding, actionListener, showEditingMenu)
    }

    override fun onBindViewHolder(holder: JobsViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }
}

class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem == newItem
    }
}