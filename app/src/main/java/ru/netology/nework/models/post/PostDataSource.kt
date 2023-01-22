package ru.netology.nework.models.post

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import java.io.IOException
import javax.inject.Inject

class PostDataSource @Inject constructor(
    private val apiService: ApiService,
    private val filterBy: Long = 0L,
    private val authUserId: Long? = null,
) : PagingSource<Long, Post>() {

    @Inject
    lateinit var appAuth: AppAuth

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            val result = when (params) {
                is LoadParams.Append -> {
                    if (filterBy == 0L) {
                        apiService.getBefore(id = params.key, count = params.loadSize)
                    } else {
                        if (filterBy == authUserId) //Получаем свою стену
                            apiService.getWallBefore(id = params.key, count = params.loadSize)
                        else //Получаем стену другого пользователя
                            apiService.getUserWallBefore(userId = filterBy, id = params.key, count = params.loadSize)
                    }
                }
                is LoadParams.Prepend -> {
                    if (filterBy == 0L)
                        apiService.getNewer(id = params.key)
                    else
                        if (filterBy == authUserId)
                            apiService.getWallNewer(id = params.key)
                        else
                            apiService.getUserWallNewer(userId = filterBy, id = params.key)
                    //return LoadResult.Page(data = emptyList(), nextKey = null, prevKey = params.key)
                }
                is LoadParams.Refresh -> {
                    if (filterBy == 0L)
                        apiService.getLatest(params.loadSize)
                    else
                        if (filterBy == authUserId)
                            apiService.getWallLatest(params.loadSize)
                        else
                            apiService.getUserWallLatest(userId = filterBy, params.loadSize)
                }
            }

            if (!result.isSuccessful)
                throw HttpException(result)

            val data = result.body().orEmpty()
            return LoadResult.Page(
                data = data,
                prevKey = params.key,
                nextKey = data.lastOrNull()?.id
            )
        } catch (e: IOException) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? = null
}