package com.future.ultimate.driver.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.future.ultimate.core.database.DatabaseFactory
import com.future.ultimate.core.database.repository.LocalDriverRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class DriverMileageReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = LocalDriverRepository(
            dao = DatabaseFactory.create(applicationContext).appDao(),
            context = applicationContext,
        )
        val registration = repository.observeSession().first()?.registration.orEmpty()
        DriverSyncNotifier.notifyMileageReminder(applicationContext, registration)
        return Result.success()
    }

    companion object {
        private const val WorkName = "driver-mileage-reminder-weekly"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriverMileageReminderWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
