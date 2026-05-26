package com.example.autobookkeeper.data

import androidx.room.TypeConverter

class DatabaseConverters {

    @TypeConverter
    fun fromBoolean(value: Boolean): Int {
        return if (value) 1 else 0
    }

    @TypeConverter
    fun toBoolean(value: Int): Boolean {
        return value == 1
    }
}