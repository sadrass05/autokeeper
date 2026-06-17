package com.example.autobookkeeper.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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

sealed class KickOnceResult {
    data object SkippedAlreadyAlive : KickOnceResult()
    data class Success(val durationMs: Long) : KickOnceResult()
    data class Failed(val reason: String) : KickOnceResult()
}

data class KickOnceStrategy(
    val shouldStartTrampoline: Boolean,
    val waitBeforeRebindMs: Long,
    val shouldRequestRebind: Boolean
) {
    companion object {
        fun determine(processAlive: Boolean, nlsAlive: Boolean): KickOnceStrategy {
            return when {
                nlsAlive -> KickOnceStrategy(
                    shouldStartTrampoline = false,
                    waitBeforeRebindMs = 0L,
                    shouldRequestRebind = false
                )
                processAlive -> KickOnceStrategy(
                    shouldStartTrampoline = false,
                    waitBeforeRebindMs = 500L,  // 短等待, 进程活着 rebind 应该立即生效
                    shouldRequestRebind = true
                )
                else -> KickOnceStrategy(
                    shouldStartTrampoline = true,
                    waitBeforeRebindMs = 3500L,  // 长等待, 冷启动 + bind
                    shouldRequestRebind = true
                )
            }
        }
    }
}

/**
 * NLS 死进程自动恢复 worker：每 5 分钟尝试一次。
 *
 * 步骤：
 * 1. 检查是否已授权通知权限 + NLS 还活着 → 已恢复则取消 worker
 * 2. 启动 [NlsTrampolineService] 拉起本 app 进程
 * 3. requestRebind 通知系统重连
 * 4. 累计失败 [MAX_ATTEMPTS] 次后放弃, 提示用户去设置
 */
@HiltWorker
class NlsRestartWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? App
            if (app == null) {
                Log.w(TAG, "无法获取 App 实例")
                return Result.success()
            }

            if (!app.isNotificationListenerEnabled()) {
                Log.w(TAG, "未授予通知权限, 停止重启尝试")
                app.clearNlsRestartAttempts()
                return Result.success()
            }

            if (App.notificationListenerRunning) {
                Log.i(TAG, "✅ NLS 已恢复, 取消重启 worker")
                app.clearNlsRestartAttempts()
                NlsRestartWorker.cancel(applicationContext)
                return Result.success()
            }

            val attempt = App.nlsRestartAttempts
            if (attempt >= MAX_ATTEMPTS) {
                Log.w(TAG, "⚠️ NLS 重启失败 ${MAX_ATTEMPTS} 次, 停止尝试, 提示用户去设置")
                app.markNlsRestartGiveUp()
                return Result.success()
            }

            Log.i(TAG, "🔄 尝试重启 NLS (第 ${attempt + 1}/${MAX_ATTEMPTS} 次)")

            // 1. 启动 trampoline service 拉起本 app 进程
            try {
                val intent = Intent(applicationContext, NlsTrampolineService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动 trampoline service 失败", e)
            }

            // 2. 等 2 秒让进程醒来, 再 requestRebind
            try {
                kotlinx.coroutines.delay(2000)
                val cls = Class.forName("android.service.notification.NotificationListenerService")
                val method = cls.getMethod("requestRebind", ComponentName::class.java)
                method.invoke(
                    null,
                    ComponentName(applicationContext, NotificationListener::class.java)
                )
                Log.i(TAG, "✅ doWork: requestRebind 已发送 (在 trampoline 唤醒进程后)")
            } catch (e: Exception) {
                Log.e(TAG, "requestRebind 失败", e)
            }

            app.recordNlsRestartAttempt()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "NLS 重启 worker 异常", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NlsRestartWorker"
        private const val MAX_ATTEMPTS = 6  // 15min * 6 = 90 分钟

        const val UNIQUE_NAME = "nls_restart"

        /**
         * 立即执行一次 NLS 重启 (不等周期性 worker 触发)
         * 步骤: 检测进程状态 → 决策 → trampoline/wait/rebind → 返回结果
         * 用于弹窗"立即尝试恢复"按钮
         */
        fun kickOnce(context: Context): KickOnceResult {
            val startTime = System.currentTimeMillis()
            return try {
                // 写"已 kick"时间戳, NlsHealthCard 显示用
                context.getSharedPreferences("nls_kick", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_kick_time", System.currentTimeMillis())
                    .apply()

                val processAlive = isProcessAlive()
                val nlsAlive = App.notificationListenerRunning
                val strategy = KickOnceStrategy.determine(processAlive, nlsAlive)

                Log.i(
                    TAG,
                    "🚀 kickOnce 开始: processAlive=$processAlive nlsAlive=$nlsAlive " +
                        "strategy=(trampoline=${strategy.shouldStartTrampoline} wait=${strategy.waitBeforeRebindMs}ms rebind=${strategy.shouldRequestRebind})"
                )

                if (nlsAlive && !strategy.shouldRequestRebind) {
                    return KickOnceResult.SkippedAlreadyAlive
                }

                if (strategy.shouldStartTrampoline) {
                    val intent = Intent(context, NlsTrampolineService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }

                if (strategy.waitBeforeRebindMs > 0) {
                    Thread.sleep(strategy.waitBeforeRebindMs)
                }

                if (strategy.shouldRequestRebind) {
                    val cls = Class.forName("android.service.notification.NotificationListenerService")
                    val method = cls.getMethod("requestRebind", ComponentName::class.java)
                    method.invoke(
                        null,
                        ComponentName(context, NotificationListener::class.java)
                    )
                }

                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ kickOnce 完成: duration=${duration}ms")
                KickOnceResult.Success(durationMs = duration)
            } catch (e: Throwable) {
                Log.e(TAG, "kickOnce 失败", e)
                KickOnceResult.Failed(reason = e.message ?: e.javaClass.simpleName)
            }
        }

        /**
         * 检测本 app 进程是否还活着
         * 原理: 检查 ApplicationContext 的 pid 是否在系统中存在
         */
        private fun isProcessAlive(): Boolean {
            return try {
                val myPid = android.os.Process.myPid()
                val am = App.instance.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val processes = am.runningAppProcesses ?: return true  // 拿不到列表, 乐观假设活着
                processes.any { it.pid == myPid }
            } catch (e: Throwable) {
                Log.w(TAG, "isProcessAlive 检测失败, 假设活着", e)
                true
            }
        }

        fun schedule(context: Context) {
            try {
                // Android 硬限制: PeriodicWorkRequest 最小周期 15 分钟
                // 5/10 分钟的设置会被静默上调
                val request = PeriodicWorkRequestBuilder<NlsRestartWorker>(15, TimeUnit.MINUTES)
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
                Log.i(TAG, "NLS 重启 worker 已调度 (15min 周期, Android 硬限制)")
            } catch (e: Exception) {
                Log.e(TAG, "调度 NLS 重启 worker 失败", e)
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
                Log.i(TAG, "NLS 重启 worker 已取消")
            } catch (e: Exception) {
                Log.e(TAG, "取消 NLS 重启 worker 失败", e)
            }
        }
    }
}
