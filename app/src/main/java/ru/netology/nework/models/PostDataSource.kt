package ru.netology.nework.models

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.ApiService
import javax.inject.Inject

const val PAGE_SIZE = 3

class PostDataSource @Inject constructor(
    private val apiService: ApiService,
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val position = params.key ?: 0
            val responseAllPosts = apiService.getAllPosts()
            val posts = responseAllPosts.body() ?: emptyList<Post>()
//            val nextKey = if(allPostsList.isEmpty()) null else position + (params.loadSize / PAGE_SIZE)
//            val response = apiService.getAfter(allPostsList.minOfOrNull { it.id } ?: 0, nextKey ?: 0)
//            val posts = response.body() ?: emptyList<Post>()
            val nextKey = null


            LoadResult.Page(
                data =  posts,
                prevKey = if (position > 0) position - 1 else null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?:
                state.closestPageToPosition(anchorPosition)?.prevKey?.minus(1)
        }
    }
}