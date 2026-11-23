package com.example.nutriplan.domain

import java.time.LocalDate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorTest {
    private val hariIni = LocalDate.of(2026, 11, 16)

    @Test fun sandi() {
        assertNull(Validator.sandi("abc12345"))
        assertNotNull(Validator.sandi("abcdefgh"))
        assertNotNull(Validator.sandi("12345678"))
        assertNotNull(Validator.sandi("ab1"))
    }

    @Test fun konfirmasi() {
        assertNull(Validator.konfirmasi("abc12345", "abc12345"))
        assertNotNull(Validator.konfirmasi("abc12345", "abc12346"))
    }

    @Test fun email() {
        assertNull(Validator.email("anis@mail.com"))
        assertNotNull(Validator.email("anis"))
        assertNotNull(Validator.email("anis@mail"))
    }

    @Test fun tinggi() {
        assertNull(Validator.tinggi("170"))
        assertNotNull(Validator.tinggi("119"))
        assertNotNull(Validator.tinggi("221"))
        assertNotNull(Validator.tinggi(""))
    }

    @Test fun berat() {
        assertNull(Validator.berat("60"))
        assertNotNull(Validator.berat("29"))
        assertNotNull(Validator.berat("201"))
    }

    @Test fun tanggalLahir() {
        assertNull(Validator.tanggalLahir(LocalDate.of(1999, 5, 14), hariIni))
        assertNotNull(Validator.tanggalLahir(null, hariIni))
        assertNotNull(Validator.tanggalLahir(LocalDate.of(2020, 1, 1), hariIni))   // usia 6
        assertNotNull(Validator.tanggalLahir(LocalDate.of(1900, 1, 1), hariIni))   // terlalu tua
        assertNotNull(Validator.tanggalLahir(LocalDate.of(2030, 1, 1), hariIni))   // masa depan
    }
}