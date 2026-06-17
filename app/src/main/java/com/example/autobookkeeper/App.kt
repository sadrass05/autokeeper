package com.example.autobookkeeper

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.autobookkeeper.backup.BackupManager
import com.example.autobookkeeper.backup.VerifyResult
import com.example.autobookkeeper.backup.WeeklyBackupWorker
import com.example.autobookkeeper.notification.NlsAlarmReceiver
import com.example.autobookkeeper.notification.NlsWatchdogWorker
import com.example.autobookkeeper.notification.Rom
import com.example.autobookkeeper.notification.RomDetector
import com.example.autobookkeeper.data.AppDatabase
import com.example.autobookkeeper.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class App : Application() {

    @Inject @Singleton
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var db: AppDatabase

    companion object {
        private var _instance: App? = null
        val instance: App get() = _instance!!
        private const val TAG = "App"

        /** 通知监听服务是否在运行 */
        @Volatile
        var notificationListenerRunning: Boolean = false

        /** NLS 心跳连续未命中的次数（看门狗累加，NLS 恢复时清零） */
        @Volatile
        var nlsWatchdogMissCount: Int = 0

        /** NLS 自动重启尝试次数 (每次 NlsRestartWorker 失败 +1, 成功时清零) */
        @Volatile
        var nlsRestartAttempts: Int = 0

        /** NLS 重启已放弃标志 (尝试 6 次后置 true, 用户去设置重勾后清零) */
        @Volatile
        var nlsRestartGiveUp: Boolean = false

        /** 当前 ROM 信息 (App.onCreate() 时检测一次, 后续全局用) */
        @Volatile
        var currentRom: Rom = Rom(type = Rom.Type.Unknown, version = "?")

        /**
         * 重置 NLS 重启计数 (companion 版, 供 UI/Service 直接调用, 无需 App 实例)
         * 修复: 之前是 `val clearNlsRestartAttempts: Any { get() { TODO() } }` 桩,
         *       getter 抛 NotImplementedError 导致按钮闪退 + worker 永远没调度
         */
        fun clearNlsRestartAttempts() {
            nlsRestartAttempts = 0
            nlsRestartGiveUp = false
        }
    }

    fun recordNlsWatchdogMiss() {
        nlsWatchdogMissCount++
    }

    fun clearNlsWatchdogMiss() {
        nlsWatchdogMissCount = 0
    }

    fun recordNlsRestartAttempt() {
        nlsRestartAttempts++
    }

    fun clearNlsRestartAttempts() {
        nlsRestartAttempts = 0
        nlsRestartGiveUp = false
    }

    fun markNlsRestartGiveUp() {
        nlsRestartGiveUp = true
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        currentRom = RomDetector.detectFromSystem()
        Log.i(TAG, "ROM 检测: type=${currentRom.type} version=${currentRom.version}")
        Log.i("AutoBookkeeper", "🚀 [App.onCreate] App启动完成, 包名=$packageName, 进程ID=${android.os.Process.myPid()}")

        if (!BuildConfig.IS_PRO) {
            ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    db.expenseDao().clearAllFinanceFlags()
                    Log.i(TAG, "Cleared all finance expense flags for standard version")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to clear finance flags", e)
                }
            }
        }

        WeeklyBackupWorker.schedule(this)
        NlsWatchdogWorker.schedule(this)
        NlsAlarmReceiver.schedule(this)  // L4 兜底: 30 分钟一次 alarm, WorkManager 被限制时也能唤醒

        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val result = backupManager.verifyDataIntegrity()
                if (result is VerifyResult.Inconsistent) {
                    Log.w(TAG, "Data integrity issue detected: ${result.suggestion}")
                } else {
                    Log.i(TAG, "Data integrity check passed")
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to verify data integrity", e)
            }
        }

        if (!isNotificationListenerEnabled()) {
            Log.w(TAG, "⚠️ 通知访问权限未启用，用户需手动授权")
        } else {
            Log.i(TAG, "✅ 通知访问权限已启用")
        }
    }

    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
