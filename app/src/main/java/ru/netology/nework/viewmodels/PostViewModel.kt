package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.models.*
import ru.netology.nework.repository.PostRemoteMediator
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val noPhoto = PhotoModel()

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val apiService: ApiService,
    private val appAuth: AppAuth,
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
) : ViewModel() {

    val authorized: Boolean
        get() = appAuth.authStateFlow.value?.token != null

    val authData: LiveData<Token?> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)

    private val _authUser: MutableLiveData<User?> = MutableLiveData(null)
    val authUser: LiveData<User?>
        get() = _authUser

    private val _changingPost: MutableLiveData<Post?> = MutableLiveData(null)
    val changingPost: LiveData<Post?>
        get() = _changingPost

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
        @OptIn(ExperimentalPagingApi::class)
        val data: Flow<PagingData<Post>> = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostDataSource(apiService) },
            remoteMediator = PostRemoteMediator(
                apiService = apiService,
                //postDao = postDao,
                postRemoteKeyDao = postRemoteKeyDao,
                appDb = appDb,
            )
        ).flow.cachedIn(viewModelScope)

        localDataFlow = combine(data, localChangesFlow, this::merge)
    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited: MutableLiveData<PostCreated?> = MutableLiveData(null)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    private fun merge(posts: PagingData<Post>, localChanges: OnChange<LocalChanges>): PagingData<PostListItem> {
        return posts
            .map { post ->
                val likedFlag = localChanges.value.likesFlags[post.id]
                val likedIds = localChanges.value.likedIds[post.id] ?: emptyList()
                val postWithLocalChanges = if (likedFlag == null) post else post.copy(likedByMe = likedFlag, likeOwnerIds = likedIds)
                PostListItem(postWithLocalChanges)
            }
    }

    fun likeById(id: Int, likeByMe: Boolean) {
        viewModelScope.launch {
            try {
                val changingPost = repository.likeById(id, likeByMe)
                localChanges.likesFlags[id] = changingPost.likedByMe
                localChanges.likedIds[id] = changingPost.likeOwnerIds
                localChangesFlow.value = OnChange(localChanges)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
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

    fun edit(post: PostCreated) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
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
                    _dataState.value = FeedModelState(error = true)
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
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }
}

class OnChange<T>(val value: T)

class LocalChanges {
    val likesFlags = mutableMapOf<Int, Boolean>()
    val likedIds = mutableMapOf<Int, List<Int>>()
}
