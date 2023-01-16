package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.PhotoModel
import ru.netology.nework.models.Token
import ru.netology.nework.repository.AuthAndRegisterRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: AuthAndRegisterRepository,
) : ViewModel() {

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val noPhoto = PhotoModel()
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    private val _registrationData: MutableLiveData<Token?> = MutableLiveData(null)
    val registrationData: LiveData<Token?>
        get() = _registrationData

    fun registration(login: String, pass: String, name: String) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                when (_photo.value) {
                    noPhoto -> _registrationData.value = repository.registration(login, pass, name)
                    else -> _photo.value?.file?.let { file ->
                        _registrationData.value = repository.registerWithPhoto(
                            login.toRequestBody("text/plain".toMediaType()),
                            pass.toRequestBody("text/plain".toMediaType()),
                            name.toRequestBody("text/plain".toMediaType()),
                            MediaUpload(file)
                        )
                    }
                }
                _photo.value = noPhoto
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _registrationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

}