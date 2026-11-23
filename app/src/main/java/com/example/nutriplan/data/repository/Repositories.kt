package com.example.nutriplan.data.repository

import com.example.nutriplan.data.local.StatusRencana
import androidx.room.withTransaction
import com.example.nutriplan.data.local.NutriDatabase
import com.example.nutriplan.data.local.WaktuMakan
import com.example.nutriplan.data.local.dao.MealPlanDao
import com.example.nutriplan.data.local.dao.MenuDao
import com.example.nutriplan.data.local.dao.PreferenceDao
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.domain.GeneratedPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class MenuRepository(private val dao: MenuDao) {
    fun observeAll(): Flow<List<MenuEntity>> = dao.observeAll()
    suspend fun getAll(): List<MenuEntity> = dao.getAll()
    suspend fun getById(id: Long): MenuEntity? = dao.getById(id)
    suspend fun tambah(menu: MenuEntity): Long = dao.insert(menu)
    suspend fun ubah(menu: MenuEntity) = dao.update(menu)
    suspend fun hapus(menu: MenuEntity) = dao.delete(menu)
}

class PreferenceRepository(private val dao: PreferenceDao) {
    fun observe(userId: Long): Flow<PreferenceEntity?> = dao.observe(userId)
    suspend fun get(userId: Long): PreferenceEntity? = dao.get(userId)
    suspend fun save(pref: PreferenceEntity) = dao.upsert(pref)
}

/** Repository bersama PRD-1, PRD-3, dan PRD-4. */
class MealPlanRepository(
    private val db: NutriDatabase,
    private val dao: MealPlanDao
) {
    fun observeActivePlan(userId: Long): Flow<MealPlanEntity?> = dao.observeActivePlan(userId)
    fun observePlans(userId: Long): Flow<List<MealPlanEntity>> = dao.observePlans(userId)
    fun observeItems(planId: Long): Flow<List<MealPlanItemEntity>> = dao.observeItems(planId)

    fun observePlan(planId: Long): Flow<MealPlanEntity?> = dao.observePlan(planId)

    fun observeAllItems(userId: Long): Flow<List<MealPlanItemEntity>> = dao.observeAllItems(userId)

    /** Salin rencana lama menjadi rencana AKTIF baru mulai [mulai]. Semua menu belum dimakan. */
    suspend fun reusePlan(planId: Long, userId: Long, mulai: LocalDate): Long? =
        db.withTransaction<Long?> {
            val lama = dao.getPlan(planId)
            val items = dao.getItems(planId)
            if (lama == null || items.isEmpty()) {
                null
            } else {
                dao.finishActivePlans(userId)
                val baru = dao.insertPlan(
                    MealPlanEntity(
                        userId = userId, nama = lama.nama,
                        tanggalMulai = mulai.toString(), durasi = lama.durasi
                    )
                )
                dao.insertItems(
                    items.map {
                        it.copy(
                            id = 0, planId = baru,
                            tanggal = mulai.plusDays(it.hari - 1L).toString(),
                            sudahDimakan = false
                        )
                    }
                )
                baru
            }
        }
    suspend fun getActivePlan(userId: Long): MealPlanEntity? =
        dao.observeActivePlan(userId).first()

    suspend fun getItem(itemId: Long): MealPlanItemEntity? = dao.getItem(itemId)

    /** Simpan hasil generator. Rencana aktif sebelumnya otomatis menjadi SELESAI. Harga & kalori disalin (snapshot). */
    suspend fun createPlan(userId: Long, nama: String, mulai: LocalDate, plan: GeneratedPlan): Long =
        db.withTransaction {
            dao.finishActivePlans(userId)
            val planId = dao.insertPlan(
                MealPlanEntity(
                    userId = userId, nama = nama,
                    tanggalMulai = mulai.toString(), durasi = plan.durasi
                )
            )
            val items = plan.items.map {
                MealPlanItemEntity(
                    planId = planId,
                    hari = it.hari,
                    tanggal = mulai.plusDays(it.hari - 1L).toString(),
                    waktuMakan = it.waktuMakan,
                    menuId = it.menu.id,
                    kaloriSnapshot = it.menu.kalori,
                    hargaSnapshot = it.menu.harga
                )
            }
            dao.insertItems(items)
            planId
        }

    /** Tambah Makanan: isi slot kosong, atau timpa menu di slot yang sama. Tidak diblokir walau melewati batas. */
    suspend fun setItem(planId: Long, hari: Int, waktu: WaktuMakan, menu: MenuEntity) {
        db.withTransaction {
            val plan = dao.getPlan(planId)
            if (plan != null) {
                val tanggal = LocalDate.parse(plan.tanggalMulai).plusDays(hari - 1L).toString()
                val ada = dao.getItems(planId).firstOrNull { it.hari == hari && it.waktuMakan == waktu }
                if (ada != null) {
                    dao.updateItem(
                        ada.copy(
                            menuId = menu.id, kaloriSnapshot = menu.kalori,
                            hargaSnapshot = menu.harga, sudahDimakan = false
                        )
                    )
                } else {
                    dao.insertItem(
                        MealPlanItemEntity(
                            planId = planId, hari = hari, tanggal = tanggal, waktuMakan = waktu,
                            menuId = menu.id, kaloriSnapshot = menu.kalori, hargaSnapshot = menu.harga
                        )
                    )
                }
            }
        }
    }

    /** Simpan status dimakan. Semua menu dimakan -> rencana SELESAI; batal centang -> kembali AKTIF (bila tak ada rencana aktif lain). */
    suspend fun getItems(planId: Long): List<MealPlanItemEntity> = dao.getItems(planId)

    /** Semua menu dimakan -> SELESAI. Batal centang -> kembali AKTIF (bila tak ada rencana aktif lain). */
    private suspend fun sinkronStatus(planId: Long, bolehAktifKembali: Boolean) {
        val plan = dao.getPlan(planId) ?: return
        val daftar = dao.getItems(planId)
        val semua = daftar.isNotEmpty() && daftar.all { it.sudahDimakan }

        if (plan.status == StatusRencana.AKTIF && semua) {
            dao.updatePlan(plan.copy(status = StatusRencana.SELESAI))
        } else if (plan.status == StatusRencana.SELESAI && !semua && bolehAktifKembali &&
            dao.observeActivePlan(plan.userId).first() == null
        ) {
            dao.updatePlan(plan.copy(status = StatusRencana.AKTIF))
        }
    }

    suspend fun markEaten(itemId: Long, eaten: Boolean) {
        db.withTransaction {
            dao.setEaten(itemId, eaten)
            val item = dao.getItem(itemId)
            if (item != null) sinkronStatus(item.planId, bolehAktifKembali = !eaten)
        }
    }

    /**
     * Ganti menu satu slot dalam SATU transaksi: menu, snapshot kalori dan harga baru,
     * status dimakan sesuai pilihan pengguna. Satu baris item = tidak mungkin terhitung ganda.
     */
    suspend fun confirmSwap(itemId: Long, menu: MenuEntity, sudahDimakan: Boolean): Boolean =
        db.withTransaction {
            val item = dao.getItem(itemId)
            if (item == null || !menu.dataComplete) {
                false
            } else {
                dao.updateItem(
                    item.copy(
                        menuId = menu.id,
                        kaloriSnapshot = menu.kalori,
                        hargaSnapshot = menu.harga,
                        sudahDimakan = sudahDimakan
                    )
                )
                sinkronStatus(item.planId, bolehAktifKembali = !sudahDimakan)
                true
            }
        }
}