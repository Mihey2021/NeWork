package ru.netology.nework.models.event

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import java.io.IOException
import javax.inject.Inject

class EventDataSource @Inject constructor(
    private val apiService: ApiService,
) : PagingSource<Long, Event>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Event> {
        try {
            val result = when (params) {
                is LoadParams.Append -> {
                    apiService.getEventsBefore(id = params.key, count = params.loadSize)
                }
                is LoadParams.Prepend -> {
                    apiService.getEventsNewer(id = params.key)
                }
                is LoadParams.Refresh -> {
                    apiService.getEventsLatest(params.loadSize)
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

    override fun getRefreshKey(state: PagingState<Long, Event>): Long? = null
}