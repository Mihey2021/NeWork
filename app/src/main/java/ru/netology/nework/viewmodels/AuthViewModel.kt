package ru.netology.nework.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.models.Token
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
) : ViewModel() {

    val authData: LiveData<Token?> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null
}