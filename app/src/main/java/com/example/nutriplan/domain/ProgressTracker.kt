package com.example.nutriplan.domain

enum class Level { AMAN, HAMPIR, MELEWATI }

data class ItemProgres(val kalori: Int, val harga: Int, val dimakan: Boolean)

data class ProgresHarian(
    val konsumsiKalori: Int,
    val konsumsiBiaya: Int,
    val rencanaKalori: Int,
    val rencanaBiaya: Int,
    val targetKalori: Int,
    val anggaran: Int,
    val levelKalori: Level,
    val levelBiaya: Level,
    val hariSelesai: Boolean
) {
    val sisaKalori: Int get() = targetKalori - konsumsiKalori
    val sisaAnggaran: Int get() = anggaran - konsumsiBiaya

    val persenKalori: Int
        get() = if (targetKalori <= 0) 0 else (konsumsiKalori * 100L / targetKalori).toInt()
    val persenBiaya: Int
        get() = if (anggaran <= 0) 0 else (konsumsiBiaya * 100L / anggaran).toInt()

    /** Level terparah dari kalori dan biaya. */
    val peringatan: Level
        get() = if (levelKalori.ordinal >= levelBiaya.ordinal) levelKalori else levelBiaya
}

object ProgressTracker {

    /** AMAN < 90%, HAMPIR 90%..100%, MELEWATI > 100%. Pakai Long agar tidak ada pembulatan desimal. */
    fun level(konsumsi: Int, batas: Int): Level = when {
        batas <= 0 -> Level.AMAN
        konsumsi > batas -> Level.MELEWATI
        konsumsi * 10L >= batas * 9L -> Level.HAMPIR
        else -> Level.AMAN
    }

    fun hitung(items: List<ItemProgres>, targetKalori: Int, anggaran: Int): ProgresHarian {
        val dimakan = items.filter { it.dimakan }
        val kalori = dimakan.sumOf { it.kalori }
        val biaya = dimakan.sumOf { it.harga }
        return ProgresHarian(
            konsumsiKalori = kalori,
            konsumsiBiaya = biaya,
            rencanaKalori = items.sumOf { it.kalori },
            rencanaBiaya = items.sumOf { it.harga },
            targetKalori = targetKalori,
            anggaran = anggaran,
            levelKalori = level(kalori, targetKalori),
            levelBiaya = level(biaya, anggaran),
            hariSelesai = items.isNotEmpty() && items.all { it.dimakan }
        )
    }
}