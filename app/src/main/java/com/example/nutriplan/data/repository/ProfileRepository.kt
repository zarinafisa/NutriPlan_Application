package com.example.nutriplan.data.repository

import com.example.nutriplan.data.local.dao.ProfileDao
import com.example.nutriplan.data.local.dao.UserDao
import com.example.nutriplan.data.local.entity.ProfileEntity
import com.example.nutriplan.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val userDao: UserDao
) {
    fun observe(userId: Long): Flow<ProfileEntity?> = profileDao.observe(userId)
    suspend fun get(userId: Long): ProfileEntity? = profileDao.get(userId)
    suspend fun save(profile: ProfileEntity) = profileDao.upsert(profile)

    suspend fun getUser(userId: Long): UserEntity? = userDao.findById(userId)

    suspend fun updateUser(userId: Long, nama: String, email: String): Result<Unit> {
        val user = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("Akun tidak ditemukan."))
        val e = email.trim().lowercase()
        val lain = userDao.findByEmail(e)
        if (lain != null && lain.id != userId) {
            return Result.failure(IllegalStateException("Email sudah dipakai akun lain."))
        }
        userDao.update(user.copy(nama = nama.trim(), email = e))
        return Result.success(Unit)
    }
}