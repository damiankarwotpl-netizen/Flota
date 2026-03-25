package com.future.ultimate.driver

import android.app.Application
import com.future.ultimate.core.database.DatabaseFactory
import com.future.ultimate.core.database.repository.LocalDriverRepository
import com.future.ultimate.driver.sync.DriverMileageReminderWorker
import com.future.ultimate.driver.sync.DriverSyncNotifier
import com.future.ultimate.driver.sync.DriverMileageSyncWorker

class DriverApp : Application() {
    lateinit var container: DriverAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DriverAppContainer(LocalDriverRepository(DatabaseFactory.create(this).appDao(), this))
        DriverSyncNotifier.ensureChannel(this)
        DriverMileageSyncWorker.schedule(this)
        DriverMileageReminderWorker.schedule(this)
    }
}
