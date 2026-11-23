package com.example.nutriplan.data

import android.content.Context
import com.example.nutriplan.data.local.NutriDatabase
import com.example.nutriplan.data.repository.AuthRepository
import com.example.nutriplan.data.repository.MealPlanRepository
import com.example.nutriplan.data.repository.MenuRepository
import com.example.nutriplan.data.repository.PreferenceRepository
import com.example.nutriplan.data.repository.ProfileRepository

/** Pengguna yang sedang login. 0 = belum ada sesi. Dimuat dari penyimpanan saat aplikasi mulai. */
object Session {
    @Volatile
    var userId: Long = 0L
}

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("nutriplan_session", Context.MODE_PRIVATE)

    init {
        Session.userId = prefs.getLong(KEY, 0L)
    }

    fun simpan(id: Long) {
        Session.userId = id
        prefs.edit().putLong(KEY, id).apply()
    }

    fun hapus() {
        Session.userId = 0L
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "user_id"
    }
}

class AppContainer(context: Context) {
    val database: NutriDatabase = NutriDatabase.get(context)
    val sessionManager = SessionManager(context)
    val menuRepository = MenuRepository(database.menuDao())
    val preferenceRepository = PreferenceRepository(database.preferenceDao())
    val mealPlanRepository = MealPlanRepository(database, database.mealPlanDao())
    val profileRepository = ProfileRepository(database.profileDao(), database.userDao())
    val authRepository = AuthRepository(database.userDao(), sessionManager)
}