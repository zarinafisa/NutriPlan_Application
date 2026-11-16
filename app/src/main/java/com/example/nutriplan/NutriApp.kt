package com.example.nutriplan

import android.app.Application
import android.util.Log
import com.example.nutriplan.data.AppContainer
import com.example.nutriplan.data.local.MenuSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NutriApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val dao = container.database.menuDao()
            MenuSeeder.seedIfEmpty(dao)
            Log.d("NutriPlan", "Jumlah menu di database: ${dao.count()}")
        }
    }
}