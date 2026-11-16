package com.example.nutriplan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nutriplan.data.local.dao.MealPlanDao
import com.example.nutriplan.data.local.dao.MenuDao
import com.example.nutriplan.data.local.dao.PreferenceDao
import com.example.nutriplan.data.local.dao.ProfileDao
import com.example.nutriplan.data.local.dao.UserDao
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.local.entity.ProfileEntity
import com.example.nutriplan.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class, ProfileEntity::class, PreferenceEntity::class,
        MenuEntity::class, MealPlanEntity::class, MealPlanItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NutriDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun menuDao(): MenuDao
    abstract fun mealPlanDao(): MealPlanDao

    companion object {
        @Volatile
        private var INSTANCE: NutriDatabase? = null

        fun get(context: Context): NutriDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriDatabase::class.java,
                    "nutriplan.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}