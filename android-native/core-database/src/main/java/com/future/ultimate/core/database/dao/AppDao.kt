package com.future.ultimate.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesOrderEntity
import com.future.ultimate.core.database.entity.ClothesOrderItemEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.ReportEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM contacts ORDER BY surname ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(entity: ContactEntity)

    @Query("SELECT * FROM contacts WHERE lower(name)=lower(:name) AND lower(surname)=lower(:surname) LIMIT 1")
    suspend fun getContact(name: String, surname: String): ContactEntity?

    @Query("DELETE FROM contacts WHERE name = :name AND surname = :surname")
    suspend fun deleteContact(name: String, surname: String)

    @Query("SELECT * FROM cars ORDER BY name, registration")
    fun observeCars(): Flow<List<CarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCar(entity: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCar(id: Long)

    @Query("UPDATE cars SET mileage = :mileage WHERE id = :id")
    suspend fun updateMileage(id: Long, mileage: Int)

    @Query("UPDATE cars SET driver = :driver WHERE id = :id")
    suspend fun updateDriver(id: Long, driver: String)

    @Query("UPDATE cars SET lastService = mileage WHERE id = :id")
    suspend fun confirmService(id: Long)

    @Query("SELECT * FROM workers ORDER BY surname, name")
    fun observeWorkers(): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorker(entity: WorkerEntity)

    @Query("SELECT * FROM workers WHERE lower(name)=lower(:name) AND lower(surname)=lower(:surname) LIMIT 1")
    suspend fun getWorkerByName(name: String, surname: String): WorkerEntity?

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorker(id: Long)

    @Query("DELETE FROM workers WHERE lower(name)=lower(:name) AND lower(surname)=lower(:surname)")
    suspend fun deleteWorkerByName(name: String, surname: String)

    @Query("SELECT * FROM plants ORDER BY name")
    fun observePlants(): Flow<List<PlantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlant(entity: PlantEntity)

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlant(id: Long)

    @Query("SELECT * FROM clothes_sizes ORDER BY surname, name")
    fun observeClothesSizes(): Flow<List<ClothesSizeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClothesSize(entity: ClothesSizeEntity)

    @Query("SELECT * FROM clothes_sizes WHERE lower(name)=lower(:name) AND lower(surname)=lower(:surname) LIMIT 1")
    suspend fun getClothesSizeByName(name: String, surname: String): ClothesSizeEntity?

    @Query("DELETE FROM clothes_sizes WHERE id = :id")
    suspend fun deleteClothesSize(id: Long)

    @Query("DELETE FROM clothes_sizes WHERE lower(name)=lower(:name) AND lower(surname)=lower(:surname)")
    suspend fun deleteClothesSizeByName(name: String, surname: String)

    @Query("SELECT * FROM clothes_orders ORDER BY id DESC")
    fun observeClothesOrders(): Flow<List<ClothesOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClothesOrder(entity: ClothesOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClothesOrderItems(entities: List<ClothesOrderItemEntity>)

    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun observeReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(entity: SettingEntity)

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?
}
