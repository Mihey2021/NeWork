package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.*
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

    private val noPhoto = MediaModel()
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<MediaModel>
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
                    else -> _photo.value?.fileDescription?.let { fileDescription ->
                        _registrationData.value = repository.registerWithPhoto(
                            login.toRequestBody("text/plain".toMediaType()),
                            pass.toRequestBody("text/plain".toMediaType()),
                            name.toRequestBody("text/plain".toMediaType()),
                            MediaUpload(Triple(fileDescription.first!!, fileDescription.second, fileDescription.third))
                        )
                    }
                }
                _photo.value = noPhoto
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _registrationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _registrationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?, attachmentType: AttachmentType = AttachmentType.IMAGE, mediaType: MediaType? = null) {
        _photo.value = MediaModel(uri, Triple(file, attachmentType, mediaType))
    }

}