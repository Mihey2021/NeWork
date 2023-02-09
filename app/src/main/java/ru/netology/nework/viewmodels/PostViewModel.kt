package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.errors.ErrorResponse
import ru.netology.nework.filter.Filters
import ru.netology.nework.models.*
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest
import ru.netology.nework.models.post.PostDataSource
import ru.netology.nework.models.post.PostListItem
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val noMedia = MediaModel()
const val PAGE_SIZE = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val apiService: ApiService,
    filters: Filters,
    private val appAuth: AppAuth,
) : ViewModel() {

    val localDataFlow: Flow<PagingData<PostListItem>>
    private val localChanges = LocalChanges()
    private val localChangesFlow = MutableStateFlow(OnChange(localChanges))

    private val filterBy = filters.filterBy.asLiveData(Dispatchers.Default)

    init {
        val data: Flow<PagingData<Post>> = filterBy.asFlow()
            .flatMapLatest {
                Pager(
                    config = PagingConfig(
                        pageSize = PAGE_SIZE,
                        initialLoadSize = PAGE_SIZE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = { PostDataSource(apiService, it, appAuth) },
                ).flow
            }
            .cachedIn(viewModelScope)

        localDataFlow = combine(data, localChangesFlow, this::merge)
    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited: MutableLiveData<PostCreateRequest?> = MutableLiveData(null)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

    private fun merge(
        posts: PagingData<Post>,
        localChanges: OnChange<LocalChanges>
    ): PagingData<PostListItem> {
        return posts
            .map { post ->
                val changingPost = localChanges.value.changingPosts[post.id]
                val postWithLocalChanges =
                    if (changingPost == null) post
                    else post.copy(
                        content = changingPost.content,
                        coords = changingPost.coords,
                        link = changingPost.link,
                        likeOwnerIds = changingPost.likeOwnerIds,
                        mentionIds = changingPost.mentionIds,
                        mentionedMe = changingPost.mentionedMe,
                        likedByMe = changingPost.likedByMe,
                        attachment = changingPost.attachment,
                        ownedByMe = changingPost.ownedByMe,
                        users = changingPost.users,
                        isPlayed = changingPost.isPlayed,
                        initInAudioPlayer = changingPost.initInAudioPlayer,
                    )
                PostListItem(postWithLocalChanges)
            }
    }

    fun likeById(id: Long, likeByMe: Boolean) {
        viewModelScope.launch {
            try {
                val changingPost = repository.likeById(id, likeByMe)
                makeChanges(changingPost)
                _dataState.value = FeedModelState()
            } catch (e: ErrorResponse) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.reason)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    private fun makeChanges(changingPost: Post) {
        localChanges.changingPosts[changingPost.id] = changingPost
        localChangesFlow.value = OnChange(localChanges)
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

    fun edit(post: PostCreateRequest) {
        edited.value = post
    }

    fun save() {
        edited.value?.let {
            _dataState.value = FeedModelState(loading = true)
            viewModelScope.launch {
                try {
                    when (_media.value) {
                        noMedia -> {
                            val changingPost = repository.save(it)
                            makeChanges(changingPost)
                        }
                        else -> _media.value?.fileDescription?.let { fileDescription ->
                            val changingPost = repository.saveWithAttachment(
                                it,
                                MediaUpload(
                                    Triple(
                                        fileDescription.first!!,
                                        fileDescription.second,
                                        fileDescription.third
                                    )
                                )
                            )
                            makeChanges(changingPost)
                        }
                    }
                    _postCreated.value = Unit
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

    fun playStopMedia(post: Post) {
        localChanges.changingPosts.values.filter { filteringPost -> filteringPost.isPlayed && filteringPost.id != post.id }
            .forEach { makeChanges(it.copy(isPlayed = false)) }
        val changingPost = post.copy(isPlayed = post.isPlayed)
        makeChanges(changingPost)
    }

    fun getMediaPlayingPost() =
        localChanges.changingPosts.values.firstOrNull { post -> post.isPlayed }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
                _dataState.value = FeedModelState(needRefresh = true)
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

class OnChange<T>(val value: T)

class LocalChanges {
    val changingPosts = mutableMapOf<Long, Post>()
}
