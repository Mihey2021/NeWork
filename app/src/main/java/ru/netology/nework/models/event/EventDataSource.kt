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

    private var maxId: Long = 0L
    private var approximateQuantity: Long = 0L

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Event> {
        try {
            maxId = apiService.getEventsLatest(1).body()?.firstOrNull()?.id ?: maxId
            //т.к. нет метода, возвращающего в диапазоне [ОТ]:[ДО], вычислим приблизительное количество ранее загруженных событий
            //(приблизительно - т.к. id хоть и отсортированы, но не строго последовательны)
            approximateQuantity = maxId.minus(params.key ?: 0L)

            if (approximateQuantity < 0) approximateQuantity = 0L

            val result = when (params) {
                is LoadParams.Append -> {
                    apiService.getEventsBefore(id = params.key, count = params.loadSize)
                }
                is LoadParams.Prepend -> {
                    apiService.getEventsLatest(params.loadSize)
                }
                is LoadParams.Refresh -> {
                    apiService.getEventsLatest(
                        if (params.key == null || params.key == approximateQuantity) params.loadSize else approximateQuantity.plus(
                            1
                        ).toInt())
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

    override fun getRefreshKey(state: PagingState<Long, Event>): Long? {
        return state.anchorPosition?.let {
            state.closestItemToPosition(it)?.id
        }
    }
}