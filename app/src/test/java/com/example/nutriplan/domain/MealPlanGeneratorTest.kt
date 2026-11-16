package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.MenuSeed
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.PreferenceEntity
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealPlanGeneratorTest {

    private val menus = MenuSeed.menus

    private fun pref(
        durasi: Int = 7,
        pola: List<String> = emptyList(),
        alergi: List<String> = emptyList(),
        anggaran: Int = 50_000,
        kalori: Int = 1800
    ) = PreferenceEntity(
        userId = 1, polaMakan = pola, alergi = alergi,
        anggaranHarian = anggaran, targetKaloriHarian = kalori, durasiHari = durasi
    )

    private fun generator(seed: Int, camilan: Boolean = false) =
        MealPlanGenerator(random = Random(seed), camilanOtomatis = camilan)

    @Test
    fun seed_berisi30MenuDanLengkap() {
        assertEquals(30, menus.size)
        assertTrue(menus.all { it.dataComplete })
    }

    @Test
    fun rencana3Dan7Hari_terbentuk() {
        for (durasi in listOf(3, 7)) {
            val plan = generator(1).generate(pref(durasi = durasi), menus).getOrThrow()
            assertEquals(durasi, plan.durasi)
            for (h in 1..durasi) assertEquals(3, plan.itemsHari(h).size)
        }
    }

    @Test
    fun menuBeralergen_tidakPernahMuncul() {
        val alergi = listOf(Alergen.KACANG, Alergen.UDANG)
        repeat(30) { seed ->
            val plan = generator(seed, camilan = true)
                .generate(pref(alergi = alergi, pola = listOf(Pola.TANPA_SUSU)), menus).getOrThrow()
            plan.items.forEach { item ->
                assertTrue(item.menu.alergen.none { it in alergi || it == Alergen.SUSU })
            }
        }
    }

    @Test
    fun totalHarian_tidakMelebihiTargetDanAnggaran() {
        repeat(30) { seed ->
            val p = pref(kalori = 1500, anggaran = 45_000)
            val plan = generator(seed, camilan = true).generate(p, menus).getOrThrow()
            for (h in 1..plan.durasi) {
                assertTrue(plan.totalKalori(h) <= p.targetKaloriHarian)
                assertTrue(plan.totalBiaya(h) <= p.anggaranHarian)
            }
        }
    }

    @Test
    fun vegetarian_semuaMenuBertagVegetarian() {
        val plan = generator(3).generate(pref(pola = listOf(Pola.VEGETARIAN)), menus).getOrThrow()
        assertTrue(plan.items.all { Pola.VEGETARIAN in it.menu.tagPola })
    }

    @Test
    fun anggaranMustahil_gagalDenganPesan_bukanCrash() {
        val result = generator(1).generate(pref(anggaran = 10_000), menus)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    @Test
    fun targetKaloriMustahil_gagalDenganPesan() {
        val result = generator(1).generate(pref(kalori = 900), menus)
        assertTrue(result.isFailure)
    }

    @Test
    fun kandidatKosong_gagalDenganPesan() {
        val result = generator(1).generate(pref(), emptyList())
        assertTrue(result.isFailure)
    }

    @Test
    fun menuTanpaKaloriAtauHarga_tidakDipakai() {
        val rusak = menus.map { if (it.waktuMakan == WaktuMakan.SIANG) it.copy(kalori = 0) else it }
        assertTrue(generator(1).generate(pref(), rusak).isFailure)
    }
}