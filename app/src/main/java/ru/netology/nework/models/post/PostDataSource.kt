package ru.netology.nework.models.post

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import java.io.IOException
import javax.inject.Inject

private var maxId: Int? = null

class PostDataSource @Inject constructor(
    private val apiService: ApiService,
) : PagingSource<Long, Post>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            val result = when (params) {
                is LoadParams.Append -> {
                    apiService.getBefore(id = params.key, count = params.loadSize)
                }
                is LoadParams.Prepend -> {
                    apiService.getNewer(id = params.key)
                    //return LoadResult.Page(data = emptyList(), nextKey = null, prevKey = params.key)
                }
                is LoadParams.Refresh -> {
                    apiService.getLatest(params.loadSize)
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