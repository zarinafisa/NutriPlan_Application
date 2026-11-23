package com.example.nutriplan.domain

import com.example.nutriplan.data.local.Alergen
import com.example.nutriplan.data.local.MenuSeed
import com.example.nutriplan.data.local.Pola
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.entity.MenuEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuReplacementEngineTest {

    private val seed = MenuSeed.menus
    private val lamaDesain = seed.first { it.nama == "Ayam Panggang Sayur" }   // 520 kkal, Rp22.000

    private fun konteksDesain() = KonteksTukar(
        menuLama = lamaDesain, kaloriLama = 520, hargaLama = 22_000,
        totalKaloriHari = 1310, totalBiayaHari = 49_000,
        targetKalori = 1800, anggaran = 50_000
    )

    private val filterDesain = SwapFilter(
        pola = setOf(Pola.SEIMBANG),
        alergi = setOf(Alergen.KACANG, Alergen.UDANG),
        kalori = RentangKalori.SEDANG,
        harga = RentangHarga.SEDANG
    )

    private fun m(
        id: Long, nama: String, kal: Int, harga: Int,
        waktu: WaktuMakan = WaktuMakan.SIANG
    ) = MenuEntity(
        id = id, nama = nama, waktuMakan = waktu, kalori = kal, protein = 10, karbo = 10, lemak = 5,
        harga = harga, bahan = listOf("x"), langkah = listOf("y"), alergen = emptyList(),
        tagPola = emptyList(), waktuMasakMenit = 10
    )

    private fun konteks(
        lama: MenuEntity, total: Int, biaya: Int, target: Int, anggaran: Int = 100_000
    ) = KonteksTukar(lama, lama.kalori, lama.harga, total, biaya, target, anggaran)

    @Test
    fun skorDanUrutan_sesuaiContohDesain() {
        val hasil = MenuReplacementEngine.findCandidates(konteksDesain(), seed, filterDesain)
        assertEquals("Ayam Panggang Nasi Merah", hasil[0].menu.nama)
        assertEquals("Nila Panggang Nasi Merah", hasil[1].menu.nama)
        assertEquals(0.0825, hasil[0].skor, 0.0005)
        assertEquals(0.1535, hasil[1].skor, 0.0005)
        assertEquals(-40, hasil[0].deltaKalori)
        assertEquals(-2_000, hasil[0].deltaHarga)
    }

    @Test
    fun waktuMakanSama_danBukanMenuLama() {
        val hasil = MenuReplacementEngine.findCandidates(konteksDesain(), seed, SwapFilter())
        assertTrue(hasil.isNotEmpty())
        assertTrue(hasil.all { it.menu.waktuMakan == WaktuMakan.SIANG })
        assertTrue(hasil.none { it.menu.id == lamaDesain.id })
    }

    @Test
    fun alergiDikecualikan() {
        val hasil = MenuReplacementEngine.findCandidates(
            konteksDesain(), seed, SwapFilter(alergi = setOf(Alergen.TELUR))
        )
        assertTrue(hasil.isNotEmpty())
        assertTrue(hasil.none { Alergen.TELUR in it.menu.alergen })
    }

    @Test
    fun vegetarianWajibBertagVegetarian() {
        val hasil = MenuReplacementEngine.findCandidates(
            konteksDesain(), seed, SwapFilter(pola = setOf(Pola.VEGETARIAN))
        )
        assertTrue(hasil.isNotEmpty())
        assertTrue(hasil.all { Pola.VEGETARIAN in it.menu.tagPola })
    }

    @Test
    fun anggaranKetat_menuMahalDikecualikan() {
        val k = konteksDesain().copy(anggaran = 49_000)
        val hasil = MenuReplacementEngine.findCandidates(k, seed, SwapFilter())
        assertTrue(hasil.none { it.menu.nama == "Nasi Udang Tumis Brokoli" })   // Rp24.000
        assertTrue(hasil.all { it.menu.harga <= 22_000 })
    }

    @Test
    fun totalKalori_melebihiTargetDikecualikan() {
        val lama = m(1, "Lama", 500, 20_000)
        val a = m(2, "A", 560, 20_000)    // total 1.560 <= 1.600
        val b = m(3, "B", 700, 20_000)    // total 1.700 > 1.600
        val hasil = MenuReplacementEngine.findCandidates(
            konteks(lama, total = 1500, biaya = 40_000, target = 1600),
            listOf(lama, a, b), SwapFilter()
        )
        assertEquals(listOf("A"), hasil.map { it.menu.nama })
    }

    @Test
    fun hariSudahMelebihi_menuLebihRinganTetapBoleh() {
        val lama = m(1, "Lama", 500, 20_000)
        val a = m(2, "A", 560, 20_000)    // lebih berat -> ditolak
        val c = m(4, "C", 450, 20_000)    // lebih ringan -> boleh
        val hasil = MenuReplacementEngine.findCandidates(
            konteks(lama, total = 1500, biaya = 40_000, target = 1000),
            listOf(lama, a, c), SwapFilter()
        )
        assertEquals(listOf("C"), hasil.map { it.menu.nama })
    }

    @Test
    fun seri_hargaLebihMurahDuluan() {
        val lama = m(1, "Lama", 500, 20_000)
        val a = m(2, "A", 450, 20_000)    // skor 0,06
        val b = m(3, "B", 500, 17_000)    // skor 0,06, lebih murah
        val hasil = MenuReplacementEngine.findCandidates(
            konteks(lama, total = 1500, biaya = 40_000, target = 3000),
            listOf(lama, a, b), SwapFilter()
        )
        assertEquals(listOf("B", "A"), hasil.map { it.menu.nama })
    }

    @Test
    fun pembagianNol_ditolak() {
        val k0 = konteksDesain().copy(kaloriLama = 0)
        val h0 = konteksDesain().copy(hargaLama = 0)
        assertTrue(MenuReplacementEngine.findCandidates(k0, seed, SwapFilter()).isEmpty())
        assertTrue(MenuReplacementEngine.findCandidates(h0, seed, SwapFilter()).isEmpty())
    }

    @Test
    fun tanpaKandidat_listKosong() {
        val hasil = MenuReplacementEngine.findCandidates(
            konteksDesain(), seed, SwapFilter(kalori = RentangKalori.TINGGI)
        )
        assertTrue(hasil.isEmpty())
    }
}