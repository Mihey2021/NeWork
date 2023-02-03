package ru.netology.nework.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.Token
import ru.netology.nework.repository.AuthAndRegisterRepository
import javax.inject.Inject

@HiltViewModel
class AuthorizationViewModel @Inject constructor(
    private val repository: AuthAndRegisterRepository,
) : ViewModel() {


    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _authorizationData: MutableLiveData<Token?> = MutableLiveData(null)
    val authorizationData: LiveData<Token?>
        get() = _authorizationData

    fun authorization(login: String, pass: String) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _authorizationData.value = repository.authentication(login, pass)
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _authorizationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _authorizationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}