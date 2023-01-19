package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.PhotoModel
import ru.netology.nework.models.Token
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventCreateRequest
import ru.netology.nework.models.event.EventDataSource
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.models.user.User
import ru.netology.nework.repository.CommonRepository
import ru.netology.nework.repository.EventsRepository
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val noPhoto = PhotoModel()

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val commonRepository: CommonRepository,
    private val apiService: ApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null

    val authData: LiveData<Token?> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)

    private val _authUser: MutableLiveData<User?> = MutableLiveData(null)
    val authUser: LiveData<User?>
        get() = _authUser

    val localDataFlow: Flow<PagingData<EventListItem>>
    private val localChanges = LocalChangesEvent()
    private val localChangesFlow = MutableStateFlow(OnChangeEvent(localChanges))

    init {
        val data: Flow<PagingData<Event>> = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { EventDataSource(apiService) },
        ).flow.cachedIn(viewModelScope)

        localDataFlow = combine(data, localChangesFlow, this::merge)
    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited: MutableLiveData<EventCreateRequest?> = MutableLiveData(null)

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit>
        get() = _eventCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    private fun merge(
        events: PagingData<Event>,
        localChanges: OnChangeEvent<LocalChangesEvent>
    ): PagingData<EventListItem> {
        return events
            .map { event ->
                val changingEvent = localChanges.value.changingEvents[event.id]
                val eventWithLocalChanges =
                    if (changingEvent == null) event
                    else event.copy(
                        content = changingEvent.content,
                        coords = changingEvent.coords,
                        link = changingEvent.link,
                        likeOwnerIds = changingEvent.likeOwnerIds,
                        speakerIds = changingEvent.speakerIds,
                        participantsIds = changingEvent.participantsIds,
                        participatedByMe = changingEvent.participatedByMe,
                        likedByMe = changingEvent.likedByMe,
                        attachment = changingEvent.attachment,
                        ownedByMe = changingEvent.ownedByMe,
                        users = changingEvent.users,
                    )
                EventListItem(eventWithLocalChanges)
            }
    }

    fun likeById(id: Long, likeByMe: Boolean) {
        viewModelScope.launch {
            try {
                val changingEvent = repository.likeEventById(id, likeByMe)
                makeChanges(changingEvent)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    private fun makeChanges(changingEvent: Event) {
        localChanges.changingEvents[changingEvent.id] = changingEvent
        localChangesFlow.value = OnChangeEvent(localChanges)
    }

    fun getUserById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _authUser.value = commonRepository.getUserById(id)
            } catch (e: Exception) {
                _authUser.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun edit(event: EventCreateRequest) {
        edited.value = event
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun changeLink(link: String) {
        val link = link.trim().ifBlank { null }
        edited.value = edited.value?.copy(link = link)
    }

    fun save() {
        edited.value?.let {
            _eventCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_photo.value) {
                        noPhoto -> repository.saveEvent(it)
                        else -> _photo.value?.file?.let { file ->
                            repository.saveWithAttachment(it, MediaUpload(file))
                        }
                    }
                    _dataState.value = FeedModelState(needRefresh = it.id == 0L)
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true, errorMessage = if (e.message?.isBlank() != false) e.stackTraceToString() else e.message)
                }
            }
        }
        edited.value = null
        _photo.value = noPhoto
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeEventById(id)
                _dataState.value = FeedModelState(needRefresh = true)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }
}

class OnChangeEvent<T>(val value: T)

class LocalChangesEvent {
    val changingEvents = mutableMapOf<Long, Event>()
}
