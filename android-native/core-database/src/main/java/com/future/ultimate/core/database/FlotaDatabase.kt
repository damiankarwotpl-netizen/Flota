package com.future.ultimate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesHistoryEntity
import com.future.ultimate.core.database.entity.ClothesOrderEntity
import com.future.ultimate.core.database.entity.ClothesOrderItemEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.ReportEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity

@Database(
    entities = [
        ContactEntity::class,
        SettingEntity::class,
        ReportEntity::class,
        PlantEntity::class,
        CarEntity::class,
        DriverAccountEntity::class,
        WorkerEntity::class,
        ClothesSizeEntity::class,
        ClothesOrderEntity::class,
        ClothesOrderItemEntity::class,
        ClothesHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FlotaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
