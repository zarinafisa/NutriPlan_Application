package com.example.nutriplan.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test fun hashBukanTeksAsli_danBisaDiverifikasi() {
        val h = PasswordHasher.hash("rahasia123")
        assertNotEquals("rahasia123", h)
        assertTrue(PasswordHasher.verify("rahasia123", h))
    }

    @Test fun sandiSalah_ditolak() {
        assertFalse(PasswordHasher.verify("salah12345", PasswordHasher.hash("rahasia123")))
    }

    @Test fun saltAcak_hashBerbedaUntukSandiSama() {
        assertNotEquals(PasswordHasher.hash("rahasia123"), PasswordHasher.hash("rahasia123"))
    }

    @Test fun formatRusak_tidakCrash() {
        assertFalse(PasswordHasher.verify("x", "bukan-hash"))
    }
}