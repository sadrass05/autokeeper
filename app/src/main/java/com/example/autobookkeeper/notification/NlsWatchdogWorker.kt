package com.example.autobookkeeper.notification

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.autobookkeeper.App
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * NLS 心跳看门狗：每 10 分钟检查一次 NLS 健康度。
 *
 * 判断逻辑（不依赖单一标志位）:
 * - 标志位 [App.notificationListenerRunning]
 * - 上次收到通知的时间（SharedPreferences 记录，由 [NotificationListener.handle] 写入）
 *
 * 任一信号异常都触发 requestRebind + 累计 missCount。
 */
@HiltWorker
class NlsWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? App
            if (app == null) {
                Log.w(TAG, "无法获取 App 实例，跳过本次心跳")
                return Result.success()
            }

            val flagRunning = App.notificationListenerRunning
            val prefs = applicationContext.getSharedPreferences("nls_health", Context.MODE_PRIVATE)
            val lastNotificationTime = prefs.getLong("last_notification_time", 0L)
            val now = System.currentTimeMillis()
            val staleness = if (lastNotificationTime > 0) now - lastNotificationTime else -1L

            Log.d(
                TAG,
                "心跳: flagRunning=$flagRunning lastNotificationAge=${
                    if (staleness >= 0) "${staleness / 1000}s" else "N/A"
                } missCount=${App.nlsWatchdogMissCount}"
            )

            if (app.isNotificationListenerEnabled()) {
                val shouldRebind = !flagRunning ||
                    (lastNotificationTime > 0L && staleness > 15 * 60 * 1000L)
                if (shouldRebind) {
                    Log.w(
                        TAG,
                        "⚠️ NLS 异常 (flagRunning=$flagRunning lastNotificationAge=${
                            if (staleness >= 0) "${staleness / 1000}s" else "N/A"
                        }), 调用 kickOnce 恢复 (含 trampoline+rebind)"
                    )
                    NlsRestartWorker.kickOnce(applicationContext)
                    app.recordNlsWatchdogMiss()
                } else if (flagRunning) {
                    if (App.nlsWatchdogMissCount > 0) {
                        Log.i(TAG, "✅ NLS 恢复正常，清零 missCount")
                    }
                    app.clearNlsWatchdogMiss()
                }
            } else {
                Log.i(TAG, "用户未授予通知权限，跳过心跳")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "心跳任务异常", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NlsWatchdogWorker"
        const val UNIQUE_NAME = "nls_watchdog"

        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<NlsWatchdogWorker>(10, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(false)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                Log.i(TAG, "NLS 心跳看门狗已调度（10min 周期）")
            } catch (e: Exception) {
                Log.e(TAG, "调度 NLS 心跳失败", e)
            }
        }
    }
}
