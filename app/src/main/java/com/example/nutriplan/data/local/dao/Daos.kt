package com.example.nutriplan.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.nutriplan.data.local.entity.MealPlanEntity
import com.example.nutriplan.data.local.entity.MealPlanItemEntity
import com.example.nutriplan.data.local.entity.MenuEntity
import com.example.nutriplan.data.local.entity.PreferenceEntity
import com.example.nutriplan.data.local.entity.ProfileEntity
import com.example.nutriplan.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun findById(id: Long): UserEntity?

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface ProfileDao {
    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE userId = :userId")
    fun observe(userId: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE userId = :userId")
    suspend fun get(userId: Long): ProfileEntity?
}

@Dao
interface PreferenceDao {
    @Upsert
    suspend fun upsert(pref: PreferenceEntity)

    @Query("SELECT * FROM preference WHERE userId = :userId")
    fun observe(userId: Long): Flow<PreferenceEntity?>

    @Query("SELECT * FROM preference WHERE userId = :userId")
    suspend fun get(userId: Long): PreferenceEntity?
}

@Dao
interface MenuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menu: MenuEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(menus: List<MenuEntity>)

    @Update
    suspend fun update(menu: MenuEntity)

    @Delete
    suspend fun delete(menu: MenuEntity)

    @Query("SELECT * FROM menu")
    suspend fun getAll(): List<MenuEntity>

    @Query("SELECT * FROM menu")
    fun observeAll(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu WHERE id = :id")
    suspend fun getById(id: Long): MenuEntity?

    @Query("SELECT COUNT(*) FROM menu")
    suspend fun count(): Int
}

@Dao
interface MealPlanDao {
    @Insert
    suspend fun insertPlan(plan: MealPlanEntity): Long

    @Insert
    suspend fun insertItems(items: List<MealPlanItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MealPlanItemEntity): Long

    @Update
    suspend fun updatePlan(plan: MealPlanEntity)

    @Update
    suspend fun updateItem(item: MealPlanItemEntity)

    @Delete
    suspend fun deleteItem(item: MealPlanItemEntity)

    @Query("UPDATE meal_plan SET status = 'SELESAI' WHERE userId = :userId AND status = 'AKTIF'")
    suspend fun finishActivePlans(userId: Long)

    @Query("SELECT * FROM meal_plan WHERE userId = :userId ORDER BY id DESC")
    fun observePlans(userId: Long): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plan WHERE userId = :userId AND status = 'AKTIF' ORDER BY id DESC LIMIT 1")
    fun observeActivePlan(userId: Long): Flow<MealPlanEntity?>

    @Query("SELECT * FROM meal_plan WHERE id = :planId")
    fun observePlan(planId: Long): Flow<MealPlanEntity?>

    @Query("SELECT i.* FROM meal_plan_item i INNER JOIN meal_plan p ON i.planId = p.id WHERE p.userId = :userId")
    fun observeAllItems(userId: Long): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan WHERE id = :planId")
    suspend fun getPlan(planId: Long): MealPlanEntity?

    @Query("SELECT * FROM meal_plan_item WHERE planId = :planId ORDER BY hari, waktuMakan")
    fun observeItems(planId: Long): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_item WHERE planId = :planId")
    suspend fun getItems(planId: Long): List<MealPlanItemEntity>

    @Query("SELECT * FROM meal_plan_item WHERE id = :itemId")
    suspend fun getItem(itemId: Long): MealPlanItemEntity?

    @Query("UPDATE meal_plan_item SET sudahDimakan = :eaten WHERE id = :itemId")
    suspend fun setEaten(itemId: Long, eaten: Boolean)
}