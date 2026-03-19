package com.future.ultimate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts", primaryKeys = ["name", "surname"])
data class ContactEntity(
    val name: String,
    val surname: String,
    val email: String = "",
    val pesel: String = "",
    val phone: String = "",
    val workplace: String = "",
    val apartment: String = "",
    val plant: String = "",
    val hireDate: String = "",
    val clothesSize: String = "",
    val shoesSize: String = "",
    val notes: String = "",
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val valText: String,
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val ok: Int,
    val fail: Int,
    val skip: Int,
    val auto: Int,
    val details: String,
)

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val city: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val notes: String = "",
)

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val registration: String,
    val driver: String = "",
    val mileage: Int = 0,
    val serviceInterval: Int = 15000,
    val lastService: Int = 0,
)

@Entity(tableName = "driver_accounts")
data class DriverAccountEntity(
    @PrimaryKey val registration: String,
    val login: String,
    val password: String,
    val driverName: String,
    val changePassword: Int = 1,
)

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val surname: String,
    val plant: String = "",
    val phone: String = "",
    val position: String = "",
    val hireDate: String = "",
)

@Entity(tableName = "clothes_sizes")
data class ClothesSizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val surname: String,
    val plant: String = "",
    val shirt: String = "",
    val hoodie: String = "",
    val pants: String = "",
    val jacket: String = "",
    val shoes: String = "",
)

@Entity(tableName = "clothes_orders")
data class ClothesOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val plant: String = "",
    val status: String = "Nowe",
    val orderDesc: String = "",
)

@Entity(tableName = "clothes_order_items")
data class ClothesOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val workerId: Long = 0,
    val name: String = "",
    val surname: String = "",
    val item: String,
    val size: String = "",
    val qty: Int = 1,
    val issued: Int = 0,
)

@Entity(tableName = "clothes_history")
data class ClothesHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long = 0,
    val name: String = "",
    val surname: String = "",
    val item: String,
    val size: String = "",
    val date: String,
)
