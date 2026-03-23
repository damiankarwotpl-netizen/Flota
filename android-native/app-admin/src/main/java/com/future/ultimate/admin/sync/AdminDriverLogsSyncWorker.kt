package com.future.ultimate.admin.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.future.ultimate.core.common.repository.DriverRemoteSettingsData
import com.future.ultimate.core.database.DatabaseFactory
import com.future.ultimate.core.database.repository.LocalAdminRepository
import java.util.concurrent.TimeUnit

class AdminDriverLogsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = LocalAdminRepository(
            dao = DatabaseFactory.create(applicationContext).appDao(),
            context = applicationContext,
        )
        return try {
            repository.importDriverRemoteLogs(DriverRemoteSettingsData())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WorkName = "admin-driver-logs-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AdminDriverLogsSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
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
