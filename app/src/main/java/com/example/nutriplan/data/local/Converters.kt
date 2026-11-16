package com.example.nutriplan.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun listToString(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun stringToList(s: String): List<String> =
        if (s.isBlank()) emptyList() else s.split("|")
}