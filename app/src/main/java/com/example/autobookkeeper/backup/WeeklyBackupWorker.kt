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
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WeeklyBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {

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
                    .setInitialDelay(1, TimeUnit.HOURS)  // 首次延迟从 24h 缩短为 1h, 让新用户更快看到备份
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(false)  // 移除电量约束: 修复电量低时备份永不执行的 bug
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        TAG,
                        ExistingPeriodicWorkPolicy.UPDATE,  // 改为 UPDATE: 代码改了能立即生效, 不会再保留旧配置
                        request
                    )

                Log.i(TAG, "Weekly backup scheduled: 7天周期, 1h首次延迟, 无电量约束, UPDATE策略")
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
