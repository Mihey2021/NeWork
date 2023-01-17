package ru.netology.nework.repository

import androidx.paging.*
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostRemoteKeyEntity
import ru.netology.nework.errors.ApiError
import ru.netology.nework.models.Post
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, Post>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Post>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    apiService.getBefore(id, state.config.pageSize)
                }
                LoadType.PREPEND -> {
                    //Отключаем
                    return MediatorResult.Success(true)
                }
                LoadType.REFRESH -> {
                    postRemoteKeyDao.max()?.let { id ->
                        apiService.getAfter(id, state.config.pageSize)
                    } ?: apiService.getLatest(state.config.pageSize)
                }

            }
            if (!result.isSuccessful)
                return MediatorResult.Error(HttpException(result))

            val body = result.body() ?: throw ApiError(result.code(), result.message())

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    body.firstOrNull()?.id ?: 0
                                )
                            )
                        )
//                        if (postDao.isEmpty()) {
//                            postRemoteKeyDao.insert(
//                                listOf(
//                                    PostRemoteKeyEntity(
//                                        PostRemoteKeyEntity.KeyType.BEFORE,
//                                        body.last().id
//                                    )
//                                )
//                            )
//                        }

                    }
                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.lastOrNull()?.id ?: 0
                            )
                        )
                    }
                    else -> Unit //LoadType.PREPEND Отключен
                }
                //postDao.insert(body.toEntity())
            }

            return MediatorResult.Success(
                body.isEmpty()
            )
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }
}