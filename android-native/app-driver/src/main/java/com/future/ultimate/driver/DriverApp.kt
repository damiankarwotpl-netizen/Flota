package com.future.ultimate.driver

import android.app.Application
import com.future.ultimate.core.database.repository.StubDriverRepository

class DriverApp : Application() {
    lateinit var container: DriverAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DriverAppContainer(StubDriverRepository())
    }
}
