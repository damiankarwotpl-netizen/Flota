package com.future.ultimate.driver.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.future.ultimate.core.database.DatabaseFactory
import com.future.ultimate.core.database.repository.LocalDriverRepository
import java.util.concurrent.TimeUnit

class DriverMileageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = LocalDriverRepository(
            dao = DatabaseFactory.create(applicationContext).appDao(),
            context = applicationContext,
        )
        val state = repository.flushPendingMileageSync()
        DriverSyncNotifier.notifySyncState(applicationContext, state)
        return if (state.pendingCount > 0) Result.retry() else Result.success()
    }

    companion object {
        private const val WorkName = "driver-mileage-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriverMileageSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
