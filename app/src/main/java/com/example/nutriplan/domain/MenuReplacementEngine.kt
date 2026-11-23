package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.entity.MenuEntity
import kotlin.math.abs
import kotlin.math.roundToLong

enum class RentangKalori(val label: String) {
    RENDAH("< 400 kkal"),
    SEDANG("400–550 kkal"),
    TINGGI("> 550 kkal");

    fun cocok(kalori: Int): Boolean = when (this) {
        RENDAH -> kalori < 400
        SEDANG -> kalori in 400..550
        TINGGI -> kalori > 550
    }

    companion object {
        fun dari(kalori: Int): RentangKalori = entries.first { it.cocok(kalori) }
    }
}

enum class RentangHarga(val label: String, val ringkas: String) {
    RENDAH("< Rp15 rb", "< Rp15 ribu"),
    SEDANG("Rp15–22 rb", "Rp15–22 ribu"),
    TINGGI("> Rp22 rb", "> Rp22 ribu");

    fun cocok(harga: Int): Boolean = when (this) {
        RENDAH -> harga < 15_000
        SEDANG -> harga in 15_000..22_000
        TINGGI -> harga > 22_000
    }

    companion object {
        fun dari(harga: Int): RentangHarga = entries.first { it.cocok(harga) }
    }
}

data class SwapFilter(
    val pola: Set<String> = emptySet(),
    val alergi: Set<String> = emptySet(),
    val kalori: RentangKalori? = null,
    val harga: RentangHarga? = null
)

/** Data menu semula dan kondisi hari itu (total dihitung dari snapshot item rencana). */
data class KonteksTukar(
    val menuLama: MenuEntity,
    val kaloriLama: Int,
    val hargaLama: Int,
    val totalKaloriHari: Int,
    val totalBiayaHari: Int,
    val targetKalori: Int,
    val anggaran: Int
)

/** deltaKalori/deltaHarga = baru - lama (negatif berarti lebih ringan/hemat). */
data class Kandidat(
    val menu: MenuEntity,
    val skor: Double,
    val deltaKalori: Int,
    val deltaHarga: Int
)

object MenuReplacementEngine {

    /** skor = 0,6 x (|dKalori| / kaloriLama) + 0,4 x (|dHarga| / hargaLama). Makin kecil makin dekat. */
    fun skor(kaloriLama: Int, hargaLama: Int, kalori: Int, harga: Int): Double =
        0.6 * (abs(kalori - kaloriLama).toDouble() / kaloriLama) +
                0.4 * (abs(harga - hargaLama).toDouble() / hargaLama)

    fun findCandidates(
        k: KonteksTukar,
        semuaMenu: List<MenuEntity>,
        filter: SwapFilter
    ): List<Kandidat> {
        // Guard pembagian nol: menu semula wajib punya kalori dan harga > 0.
        if (k.kaloriLama <= 0 || k.hargaLama <= 0) return emptyList()

        val hindari = filter.alergi.map { it.trim().lowercase() }.toSet()
        val wajibVegetarian = Pola.VEGETARIAN in filter.pola
        val polaLain = filter.pola.filter { it != Pola.VEGETARIAN }

        return semuaMenu.asSequence()
            .filter { it.id != k.menuLama.id }
            .filter { it.waktuMakan == k.menuLama.waktuMakan }
            .filter { it.dataComplete && it.alergenTerverifikasi }
            .filter { m -> m.alergen.none { it.trim().lowercase() in hindari } }
            .filter { m -> !wajibVegetarian || Pola.VEGETARIAN in m.tagPola }
            .filter { m -> polaLain.isEmpty() || m.tagPola.any { it in polaLain } }
            .filter { m -> filter.kalori?.cocok(m.kalori) ?: true }
            .filter { m -> filter.harga?.cocok(m.harga) ?: true }
            .filter { m -> muatDiHari(k, m) }
            .map { m ->
                Kandidat(
                    menu = m,
                    skor = skor(k.kaloriLama, k.hargaLama, m.kalori, m.harga),
                    deltaKalori = m.kalori - k.kaloriLama,
                    deltaHarga = m.harga - k.hargaLama
                )
            }
            .sortedWith(
                compareBy<Kandidat>(
                    { (it.skor * 1e9).roundToLong() },   // dibulatkan agar seri tidak kena selisih desimal
                    { it.menu.harga },                    // seri: lebih murah dulu
                    { it.menu.nama }
                )
            )
            .toList()
    }

    /** Total harian setelah ganti tidak boleh melewati target/anggaran, kecuali menunya tidak lebih berat/mahal dari yang lama. */
    private fun muatDiHari(k: KonteksTukar, m: MenuEntity): Boolean {
        val totalKalori = k.totalKaloriHari - k.kaloriLama + m.kalori
        val totalBiaya = k.totalBiayaHari - k.hargaLama + m.harga
        val kaloriOk = totalKalori <= k.targetKalori || m.kalori <= k.kaloriLama
        val biayaOk = totalBiaya <= k.anggaran || m.harga <= k.hargaLama
        return kaloriOk && biayaOk
    }
}