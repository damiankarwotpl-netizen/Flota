package com.future.ultimate.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesOrderEntity
import com.future.ultimate.core.database.entity.ClothesOrderItemEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ClothesHistoryEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.DriverAccountEntity
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

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    suspend fun getCar(id: Long): CarEntity?

    @Query("SELECT * FROM cars WHERE lower(trim(driver)) = lower(trim(:driver))")
    suspend fun getCarsByDriver(driver: String): List<CarEntity>

    @Query("SELECT * FROM cars WHERE upper(registration) = upper(:registration) LIMIT 1")
    suspend fun getCarByRegistration(registration: String): CarEntity?

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCar(id: Long)

    @Query("UPDATE cars SET mileage = :mileage WHERE id = :id")
    suspend fun updateMileage(id: Long, mileage: Int)

    @Query("UPDATE cars SET mileage = :mileage WHERE upper(registration) = upper(:registration)")
    suspend fun updateMileageByRegistration(registration: String, mileage: Int)

    @Query("UPDATE cars SET driver = :driver WHERE id = :id")
    suspend fun updateDriver(id: Long, driver: String)

    @Query("UPDATE cars SET lastService = mileage WHERE id = :id")
    suspend fun confirmService(id: Long)

    @Query("UPDATE cars SET lastInspectionDate = :inspectionDate WHERE id = :id")
    suspend fun updateLastInspectionDate(id: Long, inspectionDate: String)

    @Query("SELECT * FROM driver_accounts WHERE upper(registration) = upper(:registration) LIMIT 1")
    suspend fun getDriverAccountByRegistration(registration: String): DriverAccountEntity?

    @Query("SELECT * FROM driver_accounts WHERE upper(registration) = upper(:registration)")
    suspend fun getDriverAccountsByRegistration(registration: String): List<DriverAccountEntity>

    @Query(
        "SELECT * FROM driver_accounts " +
            "WHERE upper(registration) = upper(:registration) AND lower(login) = lower(:login) LIMIT 1",
    )
    suspend fun getDriverAccountByRegistrationAndLogin(registration: String, login: String): DriverAccountEntity?

    @Query(
        "SELECT * FROM driver_accounts " +
            "WHERE upper(registration) = upper(:registration) AND lower(trim(driverName)) = lower(trim(:driverName)) LIMIT 1",
    )
    suspend fun getDriverAccountByRegistrationAndDriverName(registration: String, driverName: String): DriverAccountEntity?

    @Query("SELECT * FROM driver_accounts WHERE lower(login) = lower(:login) LIMIT 1")
    suspend fun getDriverAccountByLogin(login: String): DriverAccountEntity?

    @Query("SELECT * FROM driver_accounts WHERE lower(login) = lower(:login)")
    suspend fun getDriverAccountsByLogin(login: String): List<DriverAccountEntity>

    @Query("SELECT * FROM driver_accounts WHERE lower(login) = lower(:login) AND password = :password LIMIT 1")
    suspend fun getDriverAccount(login: String, password: String): DriverAccountEntity?

    @Query("SELECT * FROM driver_accounts WHERE lower(login) = lower(:login) AND password = :password")
    suspend fun getDriverAccounts(login: String, password: String): List<DriverAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDriverAccount(entity: DriverAccountEntity)

    @Query("SELECT * FROM driver_accounts")
    fun observeDriverAccounts(): Flow<List<DriverAccountEntity>>

    @Query("UPDATE driver_accounts SET password = :password, changePassword = :changePassword WHERE upper(registration) = upper(:registration)")
    suspend fun updateDriverPassword(registration: String, password: String, changePassword: Int)

    @Query("UPDATE driver_accounts SET licenseType = :licenseType, licenseValidUntil = :validUntil WHERE upper(registration) = upper(:registration)")
    suspend fun updateDriverLicense(registration: String, licenseType: String, validUntil: String)

    @Query("DELETE FROM driver_accounts WHERE upper(registration) = upper(:registration)")
    suspend fun deleteDriverAccountByRegistration(registration: String)

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

    @Query("DELETE FROM clothes_orders WHERE id = :orderId")
    suspend fun deleteClothesOrder(orderId: Long)

    @Query("DELETE FROM clothes_order_items WHERE orderId = :orderId")
    suspend fun deleteClothesOrderItemsByOrderId(orderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClothesOrderItems(entities: List<ClothesOrderItemEntity>)

    @Query("SELECT * FROM clothes_order_items WHERE orderId = :orderId ORDER BY surname, name, item")
    fun observeClothesOrderItems(orderId: Long): Flow<List<ClothesOrderItemEntity>>

    @Query("SELECT * FROM clothes_order_items WHERE orderId = :orderId ORDER BY surname, name, item")
    suspend fun getClothesOrderItems(orderId: Long): List<ClothesOrderItemEntity>

    @Query("SELECT * FROM clothes_order_items WHERE id = :id LIMIT 1")
    suspend fun getClothesOrderItem(id: Long): ClothesOrderItemEntity?

    @Query("SELECT * FROM clothes_order_items WHERE orderId = :orderId AND COALESCE(issued, 0) = 0")
    suspend fun getUnissuedClothesOrderItems(orderId: Long): List<ClothesOrderItemEntity>

    @Query("DELETE FROM clothes_order_items WHERE id = :id")
    suspend fun deleteClothesOrderItem(id: Long)

    @Query("UPDATE clothes_orders SET status = :status WHERE id = :orderId")
    suspend fun updateClothesOrderStatus(orderId: Long, status: String)

    @Query("SELECT * FROM clothes_orders WHERE id = :id LIMIT 1")
    suspend fun getClothesOrder(id: Long): ClothesOrderEntity?

    @Query("UPDATE clothes_order_items SET issued = :issued WHERE id = :id")
    suspend fun updateClothesOrderItemIssued(id: Long, issued: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClothesHistory(entity: ClothesHistoryEntity)

    @Query("SELECT COUNT(*) FROM clothes_order_items WHERE orderId = :orderId")
    suspend fun countClothesOrderItems(orderId: Long): Int

    @Query("SELECT COUNT(*) FROM clothes_order_items WHERE orderId = :orderId AND COALESCE(issued, 0) = 1")
    suspend fun countIssuedClothesOrderItems(orderId: Long): Int

    @Query("SELECT * FROM clothes_history ORDER BY date DESC, id DESC")
    fun observeClothesHistory(): Flow<List<ClothesHistoryEntity>>

    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun observeReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: ReportEntity)

    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(entity: SettingEntity)

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("DELETE FROM cars")
    suspend fun clearCars()

    @Query("DELETE FROM driver_accounts")
    suspend fun clearDriverAccounts()

    @Query("DELETE FROM workers")
    suspend fun clearWorkers()

    @Query("DELETE FROM plants")
    suspend fun clearPlants()

    @Query("DELETE FROM clothes_sizes")
    suspend fun clearClothesSizes()

    @Query("DELETE FROM clothes_orders")
    suspend fun clearClothesOrders()

    @Query("DELETE FROM clothes_order_items")
    suspend fun clearClothesOrderItems()

    @Query("DELETE FROM clothes_history")
    suspend fun clearClothesHistory()

    @Query("DELETE FROM reports")
    suspend fun clearReports()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Query("DELETE FROM sqlite_sequence")
    suspend fun resetAutoincrement()
}
