package com.future.ultimate.admin

import android.app.Application
import com.future.ultimate.admin.sync.AdminDriverLogsSyncWorker
import com.future.ultimate.core.database.DatabaseFactory
import com.future.ultimate.core.database.repository.LocalAdminRepository

class AdminApp : Application() {
    lateinit var container: AdminAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AdminAppContainer(
            repository = LocalAdminRepository(
                dao = DatabaseFactory.create(this).appDao(),
                context = this,
            ),
        )
        AdminDriverLogsSyncWorker.schedule(this)
    }
}
