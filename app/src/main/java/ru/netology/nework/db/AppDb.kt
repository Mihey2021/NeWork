package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.entity.PostRemoteKeyEntity

@Database(
    //TODO: entities = [PostEntity::class, PostRemoteKeyEntity::class],
    entities = [PostRemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
//TODO: @TypeConverters(CoordinatesConverter::class)
abstract class AppDb : RoomDatabase() {
    //TODO: abstract fun postDao(): PostDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
}