package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import kotlin.random.Random

object GeneratorConfig {
    const val MAX_ATTEMPTS = 10   // PRD §0.8 no.5: flowchart Fungsi 1
}

class PlanException(message: String) : Exception(message)

data class GeneratedItem(val hari: Int, val waktuMakan: WaktuMakan, val menu: MenuEntity)

data class GeneratedPlan(val durasi: Int, val items: List<GeneratedItem>) {
    fun itemsHari(hari: Int) = items.filter { it.hari == hari }
    fun totalKalori(hari: Int) = itemsHari(hari).sumOf { it.menu.kalori }
    fun totalBiaya(hari: Int) = itemsHari(hari).sumOf { it.menu.harga }
}

/**
 * Menyusun rencana 3-7 hari. Catatan: porsi belum dikalikan, semua angka per porsi
 * (sesuai teks "Biaya adalah estimasi per porsi").
 */
class MealPlanGenerator(
    private val random: Random = Random.Default,
    private val maxAttempts: Int = GeneratorConfig.MAX_ATTEMPTS,
    private val camilanOtomatis: Boolean = false   // PRD §0.8 no.2: camilan opsional, slot boleh kosong
) {

    fun generate(pref: PreferenceEntity, menus: List<MenuEntity>): Result<GeneratedPlan> {
        val durasi = pref.durasiHari.coerceIn(3, 7)
        val kandidat = saring(pref, menus)

        val sarapan = kandidat.filter { it.waktuMakan == WaktuMakan.SARAPAN }
        val siang = kandidat.filter { it.waktuMakan == WaktuMakan.SIANG }
        val malam = kandidat.filter { it.waktuMakan == WaktuMakan.MALAM }
        val camilan = kandidat.filter { it.waktuMakan == WaktuMakan.CAMILAN }

        if (sarapan.isEmpty() || siang.isEmpty() || malam.isEmpty()) {
            return Result.failure(
                PlanException("Menu yang cocok belum cukup untuk menyusun rencana. Coba ubah pola makan atau alergi.")
            )
        }

        val hasil = mutableListOf<GeneratedItem>()
        var kemarin: Map<WaktuMakan, Long> = emptyMap()

        for (hari in 1..durasi) {
            val itemHari = susunHari(hari, pref, sarapan, siang, malam, camilan, kemarin)
                ?: return Result.failure(
                    PlanException("Belum ada kombinasi menu untuk hari ke-$hari yang pas dengan target kalori dan anggaran. Coba naikkan anggaran atau target kalori.")
                )
            hasil += itemHari
            kemarin = itemHari.associate { it.waktuMakan to it.menu.id }
        }
        return Result.success(GeneratedPlan(durasi, hasil))
    }

    /** Batas percobaan: maksimal [maxAttempts], tidak ada perulangan tanpa batas. */
    private fun susunHari(
        hari: Int,
        pref: PreferenceEntity,
        sarapan: List<MenuEntity>,
        siang: List<MenuEntity>,
        malam: List<MenuEntity>,
        camilan: List<MenuEntity>,
        kemarin: Map<WaktuMakan, Long>
    ): List<GeneratedItem>? {
        var percobaan = 0
        while (percobaan < maxAttempts) {
            percobaan++
            val s = pilih(sarapan, kemarin[WaktuMakan.SARAPAN])
            val l = pilih(siang, kemarin[WaktuMakan.SIANG])
            val m = pilih(malam, kemarin[WaktuMakan.MALAM])

            val kalori = s.kalori + l.kalori + m.kalori
            val biaya = s.harga + l.harga + m.harga

            if (kalori <= pref.targetKaloriHarian && biaya <= pref.anggaranHarian) {
                val item = mutableListOf(
                    GeneratedItem(hari, WaktuMakan.SARAPAN, s),
                    GeneratedItem(hari, WaktuMakan.SIANG, l),
                    GeneratedItem(hari, WaktuMakan.MALAM, m)
                )
                if (camilanOtomatis) {
                    val c = camilan.shuffled(random).firstOrNull {
                        kalori + it.kalori <= pref.targetKaloriHarian &&
                                biaya + it.harga <= pref.anggaranHarian
                    }
                    if (c != null) item += GeneratedItem(hari, WaktuMakan.CAMILAN, c)
                }
                return item
            }
        }
        return null
    }

    /** Hindari menu yang sama dengan kemarin bila ada pilihan lain. */
    private fun pilih(daftar: List<MenuEntity>, idKemarin: Long?): MenuEntity {
        val pilihan = daftar.filter { it.id != idKemarin }.ifEmpty { daftar }
        return pilihan.random(random)
    }

    /**
     * Aturan saring:
     * - menu tanpa kalori/harga, atau alergen belum terverifikasi, tidak dipakai
     * - alergi pengguna dan "Tanpa susu" dibuang berdasarkan alergen menu
     * - "Vegetarian" wajib cocok; pola lain (Tinggi protein, dst.) cukup salah satu cocok
     */
    internal fun saring(pref: PreferenceEntity, menus: List<MenuEntity>): List<MenuEntity> {
        val hindari = pref.alergi.map { it.trim().lowercase() }.toMutableSet()
        if (Pola.TANPA_SUSU in pref.polaMakan) hindari += Alergen.SUSU.lowercase()
        val wajibVegetarian = Pola.VEGETARIAN in pref.polaMakan
        val polaLain = pref.polaMakan.filter { it != Pola.VEGETARIAN && it != Pola.TANPA_SUSU }

        return menus.filter { m ->
            m.dataComplete &&
                    m.alergenTerverifikasi &&
                    m.alergen.none { it.trim().lowercase() in hindari } &&
                    (!wajibVegetarian || Pola.VEGETARIAN in m.tagPola) &&
                    (polaLain.isEmpty() || m.tagPola.any { it in polaLain })
        }
    }
}