package com.example.nutriplan.data.remote

import kotlin.coroutines.cancellation.CancellationException

class MealDbRepository(private val api: MealDbApi = MealDbClient.api) {

    /** Satu resep acak. Gagal koneksi dibungkus Result, jadi aplikasi tidak crash. */
    suspend fun resepAcak(): Result<InspirasiResep> = try {
        val hasil = api.randomMeal().meals?.firstOrNull()?.toInspirasi()
        if (hasil != null) Result.success(hasil)
        else Result.failure(IllegalStateException("Resep tidak ditemukan."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}