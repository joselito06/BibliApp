package com.jbncode.bibliaapp.data.database.entities

import androidx.room.TypeConverter
import java.util.Date

class Converts {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}