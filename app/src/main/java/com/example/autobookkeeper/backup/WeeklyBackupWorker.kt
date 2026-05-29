package com.example.autobookkeeper.backup

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.autobookkeeper.App
import java.util.concurrent.TimeUnit

class WeeklyBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val backupManager: BackupManager by lazy {
        App.instance.backupManager
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting weekly backup...")
            val result = backupManager.performWeeklyBackup()

            when (result) {
                is BackupResult.Success -> {
                    Log.i(TAG, "Weekly backup completed: ${result.fileName} (${result.count} records)")
                    Result.success(workDataOf("status" to "completed", "file" to result.fileName, "count" to result.count))
                }
                is BackupResult.Failure -> {
                    Log.w(TAG, "Weekly backup failed: ${result.error}")
                    Result.failure()
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Weekly backup error", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeeklyBackupWorker"

        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(
                    7, TimeUnit.DAYS
                )
                    .setInitialDelay(24, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)

                Log.i(TAG, "Weekly backup scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule weekly backup", e)
            }
        }

        fun triggerNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeeklyBackupWorker>()
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("${TAG}_manual", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
