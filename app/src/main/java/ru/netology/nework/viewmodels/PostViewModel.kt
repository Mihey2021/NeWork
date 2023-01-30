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
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
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

private val noPhoto = PhotoModel()
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

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

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
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    private fun makeChanges(changingPost: Post) {
        localChanges.changingPosts[changingPost.id] = changingPost
        localChangesFlow.value = OnChange(localChanges)
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun edit(post: PostCreateRequest) {
        edited.value = post
    }

    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_photo.value) {
                        noPhoto -> {
                            val changingPost = repository.save(it)
                            makeChanges(changingPost)
                        }
                        else -> _photo.value?.file?.let { file ->
                            val changingPost = repository.saveWithAttachment(it, MediaUpload(file))
                            makeChanges(changingPost)
                        }
                    }
                    _dataState.value = FeedModelState(needRefresh = it.id == 0L)
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(
                        error = true,
                        errorMessage = if (e.message?.isBlank() != false) e.stackTraceToString() else e.message
                    )
                }
            }
        }
        edited.value = null
        _photo.value = noPhoto
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
                _dataState.value = FeedModelState(needRefresh = true)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }
}

class OnChange<T>(val value: T)

class LocalChanges {
    val changingPosts = mutableMapOf<Long, Post>()
}
