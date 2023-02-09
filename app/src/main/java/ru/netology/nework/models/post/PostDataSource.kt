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
    private val appAuth: AppAuth,
) : PagingSource<Long, Post>() {

    private var maxId: Long = 0L
    private var maxUserWallId: Long = 0L
    private var approximateQuantity: Long = 0L

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        val authUserId = appAuth.getAuthorizedUserId()
        try {
            //т.к. нет метода, возвращающего в диапазоне [ОТ]:[ДО], вычислим приблизительное количество ранее загруженных постов
            //(приблизительно - т.к. id хоть и отсортированы, но не строго последовательны)
            if (filterBy != 0L) {
                maxUserWallId = if (filterBy == authUserId)
                    apiService.getWallLatest(1).body()?.firstOrNull()?.id ?: maxUserWallId
                else
                    apiService.getUserWallLatest(filterBy, 1).body()?.firstOrNull()?.id
                        ?: maxUserWallId
                approximateQuantity = maxUserWallId.minus(params.key ?: 0L)
            } else {
                maxId = apiService.getLatest(1).body()?.firstOrNull()?.id ?: maxId
                approximateQuantity = maxId.minus(params.key ?: 0L)
            }

            if (approximateQuantity < 0) approximateQuantity = 0L


            val result = when (params) {
                is LoadParams.Append -> {
                    if (filterBy == 0L) {
                        apiService.getBefore(id = params.key, count = params.loadSize)
                    } else {
                        if (filterBy == authUserId) //Получаем свою стену
                            apiService.getWallBefore(id = params.key, count = params.loadSize)
                        else //Получаем стену другого пользователя
                            apiService.getUserWallBefore(
                                userId = filterBy,
                                id = params.key,
                                count = params.loadSize
                            )
                    }
                }
                is LoadParams.Prepend -> {
                    if (filterBy == 0L)
                        apiService.getLatest(params.loadSize)
                    else
                        if (filterBy == authUserId)
                            apiService.getWallLatest(params.loadSize)
                        else
                            apiService.getUserWallLatest(userId = filterBy, params.loadSize)
                }
                is LoadParams.Refresh -> {
                    if (filterBy == 0L) {
                        apiService.getLatest(
                            if (params.key == null || params.key == approximateQuantity) params.loadSize else approximateQuantity.plus(
                                1
                            ).toInt()
                        )
                    } else {
                        if (filterBy == authUserId) {
                            apiService.getWallLatest(
                                if (params.key == null || params.key == approximateQuantity) params.loadSize else approximateQuantity.plus(
                                    1
                                ).toInt()
                            )
                        } else
                            apiService.getUserWallLatest(
                                filterBy,
                                if (params.key == null || params.key == approximateQuantity) params.loadSize else approximateQuantity.plus(
                                    1
                                ).toInt()
                            )
                    }
                }
            }

            if (!result.isSuccessful)
                throw HttpException(result)

            val data = result.body().orEmpty()

            return LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = data.lastOrNull()?.id
            )
        } catch (e: IOException) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let {
            state.closestItemToPosition(it)?.id
        }
    }
}