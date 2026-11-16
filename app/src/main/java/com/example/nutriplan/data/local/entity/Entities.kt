package com.example.nutriplan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutriplan.data.local.Aktivitas
import com.example.nutriplan.data.local.JenisKelamin
import com.example.nutriplan.data.local.StatusRencana
import com.example.nutriplan.data.local.Tujuan
import com.example.nutriplan.data.local.WaktuMakan

@Entity(tableName = "user", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val email: String,
    val passwordHash: String
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val userId: Long,
    val jenisKelamin: JenisKelamin,
    val tanggalLahir: String,             // ISO yyyy-MM-dd
    val tinggiCm: Int,
    val beratKg: Int,
    val targetBeratKg: Int,
    val aktivitas: Aktivitas,
    val tujuan: Tujuan,
    val targetKalori: Int,                // hasil hitung
    val targetKaloriManual: Int? = null   // PRD-2: disimpan terpisah
)

@Entity(tableName = "preference")
data class PreferenceEntity(
    @PrimaryKey val userId: Long,
    val tujuan: Tujuan = Tujuan.JAGA,
    val polaMakan: List<String> = emptyList(),
    val alergi: List<String> = emptyList(),
    val anggaranHarian: Int = 50_000,
    val targetKaloriHarian: Int = 1800,
    val porsi: Int = 1,
    val durasiHari: Int = 7
)

@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val waktuMakan: WaktuMakan,
    val kalori: Int,
    val protein: Int,
    val karbo: Int,
    val lemak: Int,
    val harga: Int,
    val bahan: List<String>,
    val langkah: List<String>,
    val alergen: List<String>,
    val tagPola: List<String>,
    val waktuMasakMenit: Int,
    val porsi: Int = 1,
    val fotoRes: String = "",             // nama drawable, mis. "menu_ayam_panggang"
    val alergenTerverifikasi: Boolean = true
) {
    /** PRD-4 §4.5 dataComplete */
    val dataComplete: Boolean get() = kalori > 0 && harga > 0
}

@Entity(tableName = "meal_plan")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val nama: String,
    val tanggalMulai: String,             // ISO yyyy-MM-dd
    val durasi: Int,
    val status: StatusRencana = StatusRencana.AKTIF
)

@Entity(
    tableName = "meal_plan_item",
    foreignKeys = [ForeignKey(
        entity = MealPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planId")]
)
data class MealPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val hari: Int,                        // 1..durasi
    val tanggal: String,                  // ISO
    val waktuMakan: WaktuMakan,
    val menuId: Long,
    val kaloriSnapshot: Int,              // disalin saat rencana dibuat
    val hargaSnapshot: Int,
    val sudahDimakan: Boolean = false
)