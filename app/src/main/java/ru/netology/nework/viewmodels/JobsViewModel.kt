package ru.netology.nework.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.jobs.Job
import ru.netology.nework.repository.JobsRepository
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repository: JobsRepository,
    private val appAuth: AppAuth,
) : ViewModel() {

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null

    private val _jobsData: MutableLiveData<List<Job>> = MutableLiveData()
    val jobsData: LiveData<List<Job>>
        get() = _jobsData

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    fun getMyJobs() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _jobsData.value = sortingList(repository.getMyJobs())
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun getUserJobs(userId: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _jobsData.value =
                    sortingList(repository.getUserJobs(userId))
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun saveMyJob(job: Job) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                val savedJob = repository.saveMyJob(job)
                if (job.id == 0L)
                    _jobsData.value =
                        sortingList(_jobsData.value?.plus(savedJob))
                else
                    _jobsData.value = sortingList(_jobsData.value?.map {
                        if (it.id == savedJob.id) savedJob.copy(
                            name = savedJob.name,
                            position = savedJob.position,
                            start = savedJob.start,
                            finish = savedJob.finish,
                            link = savedJob.link
                        ) else it
                    })

                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    private fun sortingList(list: List<Job>?) =
        list?.sortedByDescending { job -> job.start }

    fun removeMyJobById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                repository.removeMyJobById(id)
                _jobsData.value = _jobsData.value?.filter { it.id != id }
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}