package ru.netology.nework.models

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.ApiService
import javax.inject.Inject

const val PAGE_SIZE = 3
const val STARTING_PAGE_INDEX = 1

class PostDataSource @Inject constructor(
    private val apiService: ApiService,
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val position = params.key
            val response =
                if (position == null) {
                    apiService.getLatest(params.loadSize)
                } else {
                    apiService.getBefore(params.key ?: -1, params.loadSize)
                }
            val posts = response.body() ?: emptyList<Post>()
            val nextKey = if (posts.isEmpty()) null else posts.last().id


            LoadResult.Page(
                data = posts,
                prevKey = null,//if (position == null) null else posts.firstOrNull()?.id,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.data?.lastOrNull()?.id
        }
    }
}