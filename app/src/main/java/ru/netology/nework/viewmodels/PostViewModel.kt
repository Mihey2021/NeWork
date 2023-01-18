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
import ru.netology.nework.models.*
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val noPhoto = PhotoModel()
const val PAGE_SIZE = 1

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val apiService: ApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null

    val authData: LiveData<Token?> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)

    private val _authUser: MutableLiveData<User?> = MutableLiveData(null)
    val authUser: LiveData<User?>
        get() = _authUser

//    private val _changingPost: MutableLiveData<PostListItem?> = MutableLiveData(null)
//    val changingPost: LiveData<PostListItem?>
//        get() = _changingPost

//    @OptIn(ExperimentalCoroutinesApi::class)
//    val data: Flow<PagingData<Post>> = appAuth.authStateFlow
//        .flatMapLatest { (myId, _) ->
//            repository.data
//                .map { posts ->
//                    posts.map { it.copy(ownedByMe = it.authorId == myId) }
//                }
//        }.flowOn(Dispatchers.Default)

    val localDataFlow: Flow<PagingData<PostListItem>>
    private val localChanges = LocalChanges()
    private val localChangesFlow = MutableStateFlow(OnChange(localChanges))

    init {
        val data: Flow<PagingData<Post>> = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostDataSource(apiService) },
        ).flow.cachedIn(viewModelScope)

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

    fun likeById(id: Int, likeByMe: Boolean) {
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

    fun getUserById(id: Int) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _authUser.value = repository.getUserById(id)
            } catch (e: Exception) {
                _authUser.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun edit(post: PostCreateRequest) {
        edited.value = post
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
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_photo.value) {
                        noPhoto -> repository.save(it)
                        else -> _photo.value?.file?.let { file ->
                            repository.saveWithAttachment(it, MediaUpload(file))
                        }
                    }
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true, errorMessage = if (e.message?.isBlank() != false) e.stackTraceToString() else e.message)
                }
            }
        }
        edited.value = null
        _photo.value = noPhoto
    }

    fun removeById(id: Int) {
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
    val changingPosts = mutableMapOf<Int, Post>()
}
