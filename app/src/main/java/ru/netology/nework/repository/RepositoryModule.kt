package ru.netology.nework.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface RepositoryModule {

    @Singleton
    @Binds
    fun bindsPostRepository(impl: PostRepositoryImpl): PostRepository

    @Singleton
    @Binds
    fun bindsAuthAndRegisterRepository(imp: AuthAndRegisterRepositoryImpl): AuthAndRegisterRepository

    @Singleton
    @Binds
    fun bindsEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Singleton
    @Binds
    fun bindsCommonRepository(impl: CommonRepositoryImpl): CommonRepository

    @Singleton
    @Binds
    fun bindsJobsRepository(impl: JobsRepositoryImp): JobsRepository
}