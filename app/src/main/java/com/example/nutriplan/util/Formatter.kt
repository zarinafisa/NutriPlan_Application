package com.example.nutriplan.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatter {
    private val idLocale: Locale = Locale.forLanguageTag("id-ID")
    private val numberFmt: NumberFormat = NumberFormat.getInstance(idLocale)

    /** 1310 -> "1.310" */
    fun angka(n: Int): String = numberFmt.format(n)

    /** 49000 -> "Rp 49.000" */
    fun rupiah(n: Int): String = "Rp ${numberFmt.format(n)}"

    /** Tanpa spasi untuk teks selisih: "Rp23.000" */
    fun rupiahRapat(n: Int): String = "Rp${numberFmt.format(n)}"

    /** 1310 -> "1.310 kkal" */
    fun kkal(n: Int): String = "${angka(n)} kkal"

    /** "380 kkal • Rp 15.000" */
    fun kkalHarga(kkal: Int, harga: Int): String = "${kkal(kkal)} • ${rupiah(harga)}"

    /** 7:00 -> "07.00" (titik, bukan titik dua) */
    fun waktu(t: LocalTime): String = t.format(DateTimeFormatter.ofPattern("HH.mm"))

    /** "12–18 Oktober 2026" */
    fun rentangTanggal(mulai: LocalDate, selesai: LocalDate): String {
        val bulan = DateTimeFormatter.ofPattern("MMMM", idLocale)
        return if (mulai.month == selesai.month) {
            "${mulai.dayOfMonth}–${selesai.dayOfMonth} ${selesai.format(bulan)} ${selesai.year}"
        } else {
            "${mulai.dayOfMonth} ${mulai.format(bulan)} – ${selesai.dayOfMonth} ${selesai.format(bulan)} ${selesai.year}"
        }
    }
}