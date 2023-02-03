package ru.netology.nework.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.models.*
import ru.netology.nework.models.event.Event
import ru.netology.nework.models.event.EventCreateRequest
import ru.netology.nework.models.jobs.Job
import ru.netology.nework.models.post.Post
import ru.netology.nework.models.post.PostCreateRequest
import ru.netology.nework.models.user.User

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
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("posts/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getBefore(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getAfter(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<Post>>

    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Long): Response<Post>

    @GET("events/{id}/newer")
    suspend fun getEventsNewer(@Path("id") id: Long): Response<List<Event>>

    @GET("events/{id}/before")
    suspend fun getEventsBefore(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/after")
    suspend fun getEventsAfter(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/latest")
    suspend fun getEventsLatest(@Query("count") count: Int): Response<List<Event>>

    @POST("events/{id}/likes")
    suspend fun likeEventById(@Path("id") id: Long): Response<Event>

    @DELETE("events/{id}/likes")
    suspend fun dislikeEventById(@Path("id") id: Long): Response<Event>

    @POST("events")
    suspend fun saveEvent(@Body event: EventCreateRequest): Response<Event>

    @DELETE("events/{id}")
    suspend fun removeEventById(@Path("id") id: Long): Response<Unit>

    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>

    @POST("posts")
    suspend fun save(@Body post: PostCreateRequest): Response<Post>

    @POST("events/{event_id}/participants")
    suspend fun setParticipant(@Path("event_id") event_id: Long): Response<Event>

    @DELETE("events/{event_id}/participants")
    suspend fun removeParticipant(@Path("event_id") id: Long): Response<Event>

    @Multipart
    @POST("media")
    suspend fun media(@Part media: MultipartBody.Part): Response<Media>

    @DELETE("posts/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @GET("my/wall/{id}/newer/")
    suspend fun getWallNewer(@Path("id") id: Long): Response<List<Post>>

    @GET("my/wall/{id}/before/")
    suspend fun getWallBefore(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("my/wall/{id}/after")
    suspend fun getWallAfter(@Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("my/wall/latest")
    suspend fun getWallLatest(@Query("count") count: Int): Response<List<Post>>

    @GET("{author_id}/wall/{id}/newer/")
    suspend fun getUserWallNewer(@Path("author_id") userId: Long, @Path("id") id: Long): Response<List<Post>>

    @GET("{author_id}/wall/")
    suspend fun getUserWall(@Path("author_id") userId: Long): Response<List<Post>>

    @GET("{author_id}/wall/{id}/before/")
    suspend fun getUserWallBefore(@Path("author_id") userId: Long, @Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("{author_id}/wall/{id}/after")
    suspend fun getUserWallAfter(@Path("author_id") userId: Long, @Path("id") id: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("{author_id}/wall/latest")
    suspend fun getUserWallLatest(@Path("author_id") userId: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("my/jobs/")
    suspend fun getMyJobs(): Response<List<Job>>

    @POST("my/jobs/")
    suspend fun saveMyJob(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun removeMyJobById(@Path("id") id: Long): Response<Unit>

    @GET("{user_id}/jobs/")
    suspend fun getUserJobs(@Path("user_id") id: Long): Response<List<Job>>

}