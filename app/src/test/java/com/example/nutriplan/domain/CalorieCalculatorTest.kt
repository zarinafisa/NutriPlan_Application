package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Aktivitas
import com.example.nutriplan.data.local.JenisKelamin
import com.example.nutriplan.data.local.Tujuan
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun contohLaporan_priaSedang_turunBerat() {
        val h = CalorieCalculator.hitung(
            JenisKelamin.LAKI_LAKI, 70.0, 170.0, 25, Aktivitas.SEDANG, Tujuan.TURUN
        )
        assertEquals(1642.5, h.bmr, 0.01)
        assertEquals(2545.875, h.tdee, 0.01)
        assertEquals(2046, h.targetMentah)
        assertEquals(2000, h.target)
        assertFalse(h.disesuaikanMinimum)
    }

    @Test
    fun contohUi_perempuanRingan_jagaBerat_jadi1800() {
        val h = CalorieCalculator.hitung(
            JenisKelamin.PEREMPUAN, 60.0, 165.0, 27, Aktivitas.RINGAN, Tujuan.JAGA
        )
        assertEquals(1335.25, h.bmr, 0.01)
        assertEquals(1836, h.targetMentah)
        assertEquals(1800, h.target)
    }

    @Test
    fun batasMinimumPerempuan_1200() {
        val h = CalorieCalculator.hitung(
            JenisKelamin.PEREMPUAN, 45.0, 150.0, 30, Aktivitas.SEDENTARI, Tujuan.TURUN
        )
        assertEquals(1200, h.target)
        assertTrue(h.disesuaikanMinimum)
    }

    @Test
    fun batasMinimumLaki_1500() {
        val h = CalorieCalculator.hitung(
            JenisKelamin.LAKI_LAKI, 50.0, 160.0, 30, Aktivitas.SEDENTARI, Tujuan.TURUN
        )
        assertEquals(1500, h.target)
        assertTrue(h.disesuaikanMinimum)
    }

    @Test
    fun naikBerat_targetMentahLebihBesar500DariJaga() {
        val jaga = CalorieCalculator.hitung(
            JenisKelamin.LAKI_LAKI, 70.0, 170.0, 25, Aktivitas.SEDANG, Tujuan.JAGA
        )
        val naik = CalorieCalculator.hitung(
            JenisKelamin.LAKI_LAKI, 70.0, 170.0, 25, Aktivitas.SEDANG, Tujuan.NAIK
        )
        assertEquals(500, naik.targetMentah - jaga.targetMentah)
    }

    @Test
    fun umur_dihitungDariTanggalLahir() {
        val lahir = LocalDate.of(1999, 5, 14)
        assertEquals(26, CalorieCalculator.umur(lahir, LocalDate.of(2026, 5, 13)))
        assertEquals(27, CalorieCalculator.umur(lahir, LocalDate.of(2026, 5, 14)))
    }
}