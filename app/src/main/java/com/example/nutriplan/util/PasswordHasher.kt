package com.example.nutriplan.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Password tidak pernah disimpan mentah: PBKDF2 + salt acak. Format simpan: "salt:hash" (Base64). */
object PasswordHasher {
    private const val ITERASI = 100_000
    private const val PANJANG_KUNCI = 256

    private fun turunkan(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERASI, PANJANG_KUNCI)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val enc = Base64.getEncoder()
        return "${enc.encodeToString(salt)}:${enc.encodeToString(turunkan(password, salt))}"
    }

    fun verify(password: String, tersimpan: String): Boolean {
        val bagian = tersimpan.split(":")
        if (bagian.size != 2) return false
        return try {
            val dec = Base64.getDecoder()
            val salt = dec.decode(bagian[0])
            val diharapkan = dec.decode(bagian[1])
            MessageDigest.isEqual(diharapkan, turunkan(password, salt))
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}