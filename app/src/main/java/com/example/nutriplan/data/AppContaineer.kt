package com.example.nutriplan.data

import android.content.Context
import com.example.nutriplan.data.local.NutriDatabase
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository

/** SEMENTARA: sampai login dibuat (Tahap 5-6), semua data memakai pengguna id 1. */
object Session {
    var userId: Long = 1L
}

class AppContainer(context: Context) {
    val database: NutriDatabase = NutriDatabase.get(context)
    val menuRepository = MenuRepository(database.menuDao())
    val preferenceRepository = PreferenceRepository(database.preferenceDao())
    val mealPlanRepository = MealPlanRepository(database, database.mealPlanDao())
}