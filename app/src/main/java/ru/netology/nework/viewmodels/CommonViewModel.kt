package ru.netology.nework.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.user.User
import ru.netology.nework.repository.CommonRepository
import javax.inject.Inject

@HiltViewModel
class CommonViewModel @Inject constructor(
    private val repository: CommonRepository,
) : ViewModel() {

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _usersList = MutableLiveData<List<User>>()
    val usersList: LiveData<List<User>>
        get() = _usersList

    private val _userDetail = MutableLiveData<User>()
    val userDetail: LiveData<User>
        get() = _userDetail

    fun getAllUsersList() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                val usersList = repository.getAllUsers()
                _usersList.value = usersList
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun getUserById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                val user = repository.getUserById(id)
                _userDetail.value = user
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}