package com.example.nutriplan.domain

import java.time.LocalDate

object Batas {
    val USIA = 15..80
    val BERAT = 30..200
    val TINGGI = 120..220
}

object Validator {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun nama(s: String): String? =
        if (s.isBlank()) "Nama lengkap wajib diisi." else null

    fun email(s: String): String? =
        if (EMAIL.matches(s.trim())) null else "Email tidak valid."

    fun sandi(s: String): String? =
        if (s.length >= 8 && s.any { it.isLetter() } && s.any { it.isDigit() }) null
        else "Minimal 8 karakter, huruf dan angka."

    fun konfirmasi(sandi: String, konfirmasi: String): String? =
        if (konfirmasi.isNotEmpty() && sandi == konfirmasi) null else "Kata sandi tidak sama."

    fun tinggi(s: String): String? {
        val n = s.trim().toIntOrNull()
        return if (n != null && n in Batas.TINGGI) null
        else "Isi ${Batas.TINGGI.first}–${Batas.TINGGI.last} cm."
    }

    fun berat(s: String): String? {
        val n = s.trim().toIntOrNull()
        return if (n != null && n in Batas.BERAT) null
        else "Isi ${Batas.BERAT.first}–${Batas.BERAT.last} kg."
    }

    fun tanggalLahir(d: LocalDate?, hariIni: LocalDate = LocalDate.now()): String? = when {
        d == null -> "Pilih tanggal lahir."
        d.isAfter(hariIni) -> "Tanggal tidak valid. Gunakan tanggal yang ada."
        CalorieCalculator.umur(d, hariIni) !in Batas.USIA ->
            "Usia harus ${Batas.USIA.first}–${Batas.USIA.last} tahun."
        else -> null
    }
}