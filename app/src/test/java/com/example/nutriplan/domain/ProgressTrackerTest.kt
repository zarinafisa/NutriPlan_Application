package com.example.nutriplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTrackerTest {

    @Test
    fun ambang90Persen_tepat() {
        assertEquals(Level.AMAN, ProgressTracker.level(899, 1000))
        assertEquals(Level.HAMPIR, ProgressTracker.level(900, 1000))
        assertEquals(Level.HAMPIR, ProgressTracker.level(1000, 1000))
        assertEquals(Level.MELEWATI, ProgressTracker.level(1001, 1000))
    }

    @Test
    fun batasNol_tidakCrash() {
        assertEquals(Level.AMAN, ProgressTracker.level(500, 0))
    }

    @Test
    fun hanyaMenuYangDimakanYangDihitung() {
        val items = listOf(
            ItemProgres(380, 15_000, true),
            ItemProgres(520, 22_000, false),
            ItemProgres(410, 12_000, false)
        )
        val p = ProgressTracker.hitung(items, 1800, 50_000)
        assertEquals(380, p.konsumsiKalori)
        assertEquals(15_000, p.konsumsiBiaya)
        assertEquals(1310, p.rencanaKalori)
        assertEquals(49_000, p.rencanaBiaya)
        assertEquals(1420, p.sisaKalori)
        assertEquals(35_000, p.sisaAnggaran)
        assertEquals(Level.AMAN, p.peringatan)
        assertFalse(p.hariSelesai)
    }

    @Test
    fun menandaiSemua_hariSelesai_danBiaya98Persen_hampir() {
        val items = listOf(
            ItemProgres(380, 15_000, true),
            ItemProgres(520, 22_000, true),
            ItemProgres(410, 12_000, true)
        )
        val p = ProgressTracker.hitung(items, 1800, 50_000)
        assertTrue(p.hariSelesai)
        assertEquals(1310, p.konsumsiKalori)
        assertEquals(Level.AMAN, p.levelKalori)
        assertEquals(Level.HAMPIR, p.levelBiaya)
        assertEquals(Level.HAMPIR, p.peringatan)
        assertEquals(98, p.persenBiaya)
    }

    @Test
    fun biayaMelewati_sisaNegatif() {
        val p = ProgressTracker.hitung(listOf(ItemProgres(500, 73_000, true)), 1800, 50_000)
        assertEquals(Level.MELEWATI, p.levelBiaya)
        assertEquals(Level.MELEWATI, p.peringatan)
        assertEquals(-23_000, p.sisaAnggaran)
    }

    @Test
    fun membatalkanCentang_mengembalikanAngka() {
        val dua = listOf(ItemProgres(380, 15_000, true), ItemProgres(520, 22_000, true))
        val satu = listOf(ItemProgres(380, 15_000, true), ItemProgres(520, 22_000, false))
        assertEquals(900, ProgressTracker.hitung(dua, 1800, 50_000).konsumsiKalori)
        assertEquals(380, ProgressTracker.hitung(satu, 1800, 50_000).konsumsiKalori)
    }

    @Test
    fun daftarKosong_bukanHariSelesai() {
        assertFalse(ProgressTracker.hitung(emptyList(), 1800, 50_000).hariSelesai)
    }
}