package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okhttp3.MediaType
import ru.netology.nework.api.ApiService
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.models.AttachmentType
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.MediaUpload
import ru.netology.nework.models.MediaModel
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventCreateRequest
import ru.netology.nework.models.event.EventDataSource
import ru.netology.nework.models.event.EventListItem
import ru.netology.nework.repository.EventsRepository
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import kotlin.collections.mutableMapOf
import kotlin.collections.set

private val noMedia = MediaModel()

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val apiService: ApiService,
) : ViewModel() {

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

    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

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
                        isPlayed = changingEvent.isPlayed,
                        initInAudioPlayer = changingEvent.initInAudioPlayer,
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
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    private fun makeChanges(changingEvent: Event) {
        localChanges.changingEvents[changingEvent.id] = changingEvent
        localChangesFlow.value = OnChangeEvent(localChanges)
    }

    fun changeMedia(
        uri: Uri?,
        file: File?,
        attachmentType: AttachmentType? = null,
        mediaType: MediaType? = null
    ) {
        if (file == null || attachmentType == null) return
        _media.value = MediaModel(uri, Triple(file, attachmentType, mediaType))
    }

    fun clearMedia() {
        _media.value = MediaModel()
    }

    fun edit(event: EventCreateRequest) {
        edited.value = event
    }

    fun save() {
        edited.value?.let {
            _dataState.value = FeedModelState(loading = true)
            viewModelScope.launch {
                try {
                    when (_media.value) {
                        noMedia -> {
                            val changingEvent = repository.saveEvent(it)
                            makeChanges(changingEvent)
                        }
                        else -> _media.value?.fileDescription?.let { fileDescription ->
                            val changingEvent = repository.saveWithAttachment(
                                it,
                                MediaUpload(
                                    Triple(
                                        fileDescription.first!!,
                                        fileDescription.second,
                                        fileDescription.third
                                    )
                                )
                            )
                            makeChanges(changingEvent)
                        }
                    }
                    _eventCreated.value = Unit
                    edited.value = null
                    _media.value = noMedia
                    _dataState.value = FeedModelState(needRefresh = true)
                } catch (e: ErrorResponse) {
                    _dataState.value = FeedModelState(
                        error = true,
                        errorMessage = e.reason
                    )
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(
                        error = true,
                        errorMessage = if (e.message?.isBlank() != false) e.stackTraceToString() else e.message
                    )
                }
            }
        }
    }

    fun playStopMedia(event: Event) {
        localChanges.changingEvents.values.filter { filteringEvent -> filteringEvent.isPlayed && filteringEvent.id != event.id }
            .forEach { makeChanges(it.copy(isPlayed = false)) }
        val changingEvent = event.copy(isPlayed = event.isPlayed)
        makeChanges(changingEvent)
    }

    fun getMediaPlayingEvent() =
        localChanges.changingEvents.values.firstOrNull { event -> event.isPlayed }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeEventById(id)
                _dataState.value = FeedModelState(needRefresh = true)
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun removeParticipant(eventId: Long) {
        viewModelScope.launch {
            try {
                val changingEvent = repository.removeParticipant(eventId)
                makeChanges(changingEvent)
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun setParticipant(eventId: Long) {
        viewModelScope.launch {
            try {
                val changingEvent = repository.setParticipant(eventId)
                makeChanges(changingEvent)
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun clearFeedModelState() {
        _dataState.value = FeedModelState()
    }

}

class OnChangeEvent<T>(val value: T)

class LocalChangesEvent {
    val changingEvents = mutableMapOf<Long, Event>()
}
