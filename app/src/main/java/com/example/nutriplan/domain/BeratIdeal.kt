package com.example.nutriplan.domain

import java.util.Locale

data class RentangBerat(val min: Int, val max: Int) {
    operator fun contains(kg: Int) = kg in min..max
}

enum class KategoriBmi(val label: String) {
    KURUS("Kurus"),
    NORMAL("Normal"),
    BERAT_LEBIH("Berat badan lebih"),
    OBESITAS("Obesitas")
}

/**
 * Berat badan ideal berdasarkan IMT 18,5–25,0 (batas normal Kemenkes RI).
 * Panduan umum, bukan diagnosis medis. Atlet atau orang berotot bisa berbeda.
 */
object BeratIdeal {
    const val BMI_MIN = 18.5
    const val BMI_MAX = 25.0
    private const val BMI_OBESITAS = 27.0

    /** min = bulatkan ke atas (18,5 × tinggi²), max = bulatkan ke bawah (25 × tinggi²). */
    fun rentang(tinggiCm: Int): RentangBerat {
        val kuadrat = tinggiCm * tinggiCm
        val min = (185 * kuadrat + 99_999) / 100_000
        val max = (25 * kuadrat) / 10_000
        return RentangBerat(min, max)
    }

    fun bmi(beratKg: Int, tinggiCm: Int): Double {
        val m = tinggiCm / 100.0
        return beratKg / (m * m)
    }

    fun kategori(bmi: Double): KategoriBmi = when {
        bmi < BMI_MIN -> KategoriBmi.KURUS
        bmi <= BMI_MAX -> KategoriBmi.NORMAL
        bmi <= BMI_OBESITAS -> KategoriBmi.BERAT_LEBIH
        else -> KategoriBmi.OBESITAS
    }

    fun format(bmi: Double): String =
        String.format(Locale.forLanguageTag("id-ID"), "%.1f", bmi)

    /** Pesan galat bila target di luar rentang ideal. null = lolos (atau datanya belum bisa dicek). */
    fun cekTarget(target: String, tinggi: String): String? {
        val t = target.trim().toIntOrNull() ?: return null
        val cm = tinggi.trim().toIntOrNull() ?: return null
        if (cm !in Batas.TINGGI) return null
        val r = rentang(cm)
        return if (t in r) null
        else "Target $t kg bukan berat badan ideal untuk tinggi $cm cm. Pilih antara ${r.min}–${r.max} kg."
    }
}