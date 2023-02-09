package ru.netology.nework.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import ru.netology.nework.BuildConfig
import ru.netology.nework.BuildConfig.BASE_URL
import ru.netology.nework.auth.AppAuth
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ApiModule {
    @Singleton
    @Provides
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Singleton
    @Provides
    fun provideAuthInterceptor(
        appAuth: AppAuth,
    ): Interceptor = Interceptor { chain ->
        val request = appAuth.authStateFlow.value?.token?.let {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", it)
                .build()
        } ?: chain.request()
        chain.proceed(request)
    }

    @Singleton
    @Provides
    fun provideOkHttp(
        logging: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .writeTimeout(30, TimeUnit.MINUTES)
        .readTimeout(30, TimeUnit.MINUTES)
        .build()

    @Singleton
    @Provides
    fun provideRetrofit(
        okhttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .client(okhttpClient)
        .build()

    @Singleton
    @Provides
    fun provideApiService(
        retrofit: Retrofit,
    ): ApiService = retrofit.create()
}