package com.example.nutriplan.data.repository

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

    suspend fun markEaten(itemId: Long, eaten: Boolean) = dao.setEaten(itemId, eaten)
}