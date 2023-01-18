//package ru.netology.nework.maps
//
//import android.content.Context
//import com.yandex.mapkit.MapKitFactory
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@InstallIn(SingletonComponent::class)
//@Module
//class YandexMapModule {
//    @Singleton
//    @Provides
//    fun provideYandexMap(
//        @ApplicationContext
//        context: Context,
//    ) = MapKitFactory.initialize(context)
//
//    @Singleton
//    @Provides
//    fun provideMapKitFactory() = MapKitFactory.getInstance()
//}