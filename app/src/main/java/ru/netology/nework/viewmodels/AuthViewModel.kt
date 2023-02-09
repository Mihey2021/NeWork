package ru.netology.nework.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.Token
import ru.netology.nework.models.user.User
import ru.netology.nework.repository.CommonRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val commonRepository: CommonRepository,
) : ViewModel() {

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null

    val authData: LiveData<Token?> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)

    private val _authUser: MutableLiveData<User?> = MutableLiveData(null)
    val authUser: LiveData<User?>
        get() = _authUser

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    fun clearAuthUser() {
        _authUser.value = null
    }

    fun getUserById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _authUser.value = commonRepository.getUserById(id)
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _authUser.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _authUser.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}