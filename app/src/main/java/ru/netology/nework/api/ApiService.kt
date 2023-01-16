package ru.netology.nework.api

import androidx.paging.PagingSource
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.models.*

interface ApiService {
    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun authentication(
        @Field("login") login: String,
        @Field("password") password: String
    ): Response<Token>

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun registration(
        @Field("login") login: String,
        @Field("password") password: String,
        @Field("name") name: String
    ): Response<Token>

    @Multipart
    @POST("users/registration")
    suspend fun registerWithPhoto(
        @Part("login") login: RequestBody,
        @Part("password") password: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<Token>

    @GET("posts")
    suspend fun getAllPosts(): Response<List<Post>>//PostsResponse
//    @GET("posts")
//    fun getAllPosts(): PagingSource<Int, Post>//Response<List<Post>>

    @GET("posts/{id}/newer")
    suspend fun getNewer(@Path("id") id: Int): Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getBefore(@Path("id") id: Int, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getAfter(@Path("id") id: Int, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<Post>>

    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Int): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Int): Response<Post>

    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<User>

    @POST("posts")
    suspend fun save(@Body post: PostCreated): Response<Post>

    @Multipart
    @POST("media")
    suspend fun upload(@Part media: MultipartBody.Part): Response<Media>

    @DELETE("posts/{id}")
    suspend fun removeById(@Path("id") id: Int): Response<Unit>
}