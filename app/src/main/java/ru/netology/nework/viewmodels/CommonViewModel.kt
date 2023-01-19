package ru.netology.nework.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.user.User
import ru.netology.nework.repository.AuthAndRegisterRepository
import ru.netology.nework.repository.CommonRepository
import java.util.concurrent.Flow
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

    fun getAllUsersList() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                val usersList = repository.getAllUsers()
                _usersList.value = usersList
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }
}