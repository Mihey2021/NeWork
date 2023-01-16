package ru.netology.nework.entity.convertors

import androidx.room.TypeConverter
import ru.netology.nework.models.Coordinates
//TODO:
//class CoordinatesConverter {
//    @TypeConverter
//    fun fromCoordinates(coordinates: Coordinates?): String {
//        return coordinates?.toString() ?: ""
//    }
//
//    @TypeConverter
//    fun toCoordinates(data: String): Coordinates? {
//        val splitData = data.split(";")
//        return if (splitData.isEmpty()) null else Coordinates(splitData[0], splitData[1])
//    }
//}