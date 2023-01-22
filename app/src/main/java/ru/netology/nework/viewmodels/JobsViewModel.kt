package ru.netology.nework.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
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

    private val _jobsData: MutableLiveData<List<Job>> = MutableLiveData(null)
    val jobsData: LiveData<List<Job>>
        get() = _jobsData

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    fun getMyJobs() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _jobsData.value = repository.getMyJobs().sortedByDescending { it.start }
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun getUserJobs(userId: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _jobsData.value = repository.getUserJobs(userId).sortedByDescending { it.start }
                _dataState.value = FeedModelState()
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
                _dataState.value = FeedModelState(needRefresh = true)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun removeMyJobById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                repository.removeMyJobById(id)
                _dataState.value = FeedModelState(needRefresh = true)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}