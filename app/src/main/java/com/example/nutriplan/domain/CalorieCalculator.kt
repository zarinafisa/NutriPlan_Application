package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Aktivitas
import com.example.nutriplan.data.local.JenisKelamin
import com.example.nutriplan.data.local.Tujuan
import java.time.LocalDate
import java.time.Period
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class HasilKalori(
    val bmr: Double,
    val tdee: Double,
    val targetMentah: Int,          // sesuai rumus laporan
    val target: Int,                // dibulatkan ke bawah per 100 kkal (dipakai aplikasi)
    val disesuaikanMinimum: Boolean // true bila kena batas minimum
)

object CalorieCalculator {
    const val MIN_PEREMPUAN = 1200
    const val MIN_LAKI = 1500
    private const val PEMBULATAN = 100

    fun bmr(sex: JenisKelamin, kg: Double, cm: Double, age: Int): Double =
        10 * kg + 6.25 * cm - 5 * age + if (sex == JenisKelamin.LAKI_LAKI) 5 else -161

    fun tdee(bmr: Double, level: Aktivitas): Double = bmr * level.faktor

    fun hitung(
        sex: JenisKelamin, kg: Double, cm: Double, age: Int,
        level: Aktivitas, goal: Tujuan
    ): HasilKalori {
        val b = bmr(sex, kg, cm, age)
        val t = tdee(b, level)
        val raw = when (goal) {
            Tujuan.TURUN -> t - 500
            Tujuan.JAGA -> t
            Tujuan.NAIK -> t + 500
        }
        val min = if (sex == JenisKelamin.PEREMPUAN) MIN_PEREMPUAN else MIN_LAKI
        val aman = max(raw, min.toDouble())
        return HasilKalori(
            bmr = b,
            tdee = t,
            targetMentah = aman.roundToInt(),
            target = (floor(aman / PEMBULATAN) * PEMBULATAN).toInt(),
            disesuaikanMinimum = raw < min
        )
    }

    fun umur(lahir: LocalDate, hariIni: LocalDate = LocalDate.now()): Int =
        Period.between(lahir, hariIni).years
}