package com.example.nutriplan.data.repository

import com.example.nutriplan.data.Session
import com.example.nutriplan.data.SessionManager
import com.example.nutriplan.data.local.dao.UserDao
import com.example.nutriplan.data.local.entity.UserEntity
import com.example.nutriplan.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val session: SessionManager
) {
    suspend fun register(nama: String, email: String, sandi: String): Result<Long> {
        val e = email.trim().lowercase()
        if (userDao.findByEmail(e) != null) {
            return Result.failure(IllegalStateException("Email sudah terdaftar."))
        }
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(sandi) }
        val id = userDao.insert(UserEntity(nama = nama.trim(), email = e, passwordHash = hash))
        return Result.success(id)
    }

    /** Dipakai layar Masuk (Tahap 6). */
    suspend fun login(email: String, sandi: String): Result<UserEntity> {
        val gagal = Result.failure<UserEntity>(IllegalStateException("Email atau kata sandi salah."))
        val user = userDao.findByEmail(email.trim().lowercase()) ?: return gagal
        val cocok = withContext(Dispatchers.Default) { PasswordHasher.verify(sandi, user.passwordHash) }
        if (!cocok) return gagal
        session.simpan(user.id)
        return Result.success(user)
    }

    fun masukSebagai(id: Long) = session.simpan(id)

    fun logout() = session.hapus()

    suspend fun currentUser(): UserEntity? =
        Session.userId.takeIf { it != 0L }?.let { userDao.findById(it) }
}