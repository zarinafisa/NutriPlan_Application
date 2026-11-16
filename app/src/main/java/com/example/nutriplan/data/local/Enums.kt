package com.example.nutriplan.data.local

enum class WaktuMakan(val label: String, val jam: String) {
    SARAPAN("Sarapan", "07.00"),
    SIANG("Makan Siang", "12.30"),   // PRD §0.8 no.4: 12.30, bukan 15.30
    CAMILAN("Camilan", "15.30"),
    MALAM("Makan Malam", "19.00")
}

enum class Tujuan { JAGA, TURUN, NAIK }

enum class JenisKelamin { PEREMPUAN, LAKI_LAKI }

enum class Aktivitas(val faktor: Double) {
    SEDENTARI(1.2), RINGAN(1.375), SEDANG(1.55), AKTIF(1.725), SANGAT_AKTIF(1.9)
}

enum class StatusRencana { AKTIF, SELESAI }