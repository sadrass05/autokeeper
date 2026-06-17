package com.example.autobookkeeper.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autobookkeeper.App
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.data.repository.InsertResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    private val parser by lazy { PaymentParser() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectedAt: Long = 0L

    @Volatile
    private var isDestroying: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.i("AutoBookkeeper", "📌 [NLS] onCreate pid=${android.os.Process.myPid()}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedAt = System.currentTimeMillis()
        startForegroundNotification()
        App.notificationListenerRunning = true
        // NLS 恢复时清零所有重启/watchdog 计数 + 取消重启 worker
        if (App.nlsRestartAttempts > 0 || App.nlsRestartGiveUp) {
            try {
                App.clearNlsRestartAttempts()
                NlsRestartWorker.cancel(applicationContext)
                Log.i("AutoBookkeeper", "✅ NLS 恢复, 清零重启计数并取消 worker")
            } catch (e: Throwable) {
                Log.e("AutoBookkeeper", "清零 NLS 重启计数失败", e)
            }
        }
        Log.i("AutoBookkeeper", "✅ [NLS] onListenerConnected connectedAt=$connectedAt")
        scope.launch {
            try {
                getActiveNotifications()?.let { all ->
                    val max = 50
                    val toProcess: List<StatusBarNotification> = if (all.size > max) {
                        Log.w("AutoBookkeeper", "⚠️ 启动扫描: 通知总数=${all.size}, 截断到 $max")
                        all.toList().take(max)
                    } else {
                        all.toList()
                    }
                    Log.i("AutoBookkeeper", "📥 启动扫描: 处理 ${toProcess.size} 条通知")
                    toProcess.forEach { sbn -> handle(sbn) }
                }
            } catch (e: Exception) {
                Log.e("AutoBookkeeper", "启动扫描失败", e)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val uptime = if (connectedAt > 0) (System.currentTimeMillis() - connectedAt) / 1000 else -1
        Log.w("AutoBookkeeper", "⚠️ [NLS] onListenerDisconnected uptime=${uptime}s, 尝试重连...")
        App.notificationListenerRunning = false
        requestRebind(android.content.ComponentName(this, NotificationListener::class.java))
        // 主动重建 foreground 状态, 触发系统重连
        scope.launch {
            try {
                stopForeground(STOP_FOREGROUND_DETACH)
                delay(500)
                startForegroundNotification()
            } catch (e: Exception) {
                Log.e("AutoBookkeeper", "重连失败", e)
            }
        }
    }

    override fun onDestroy() {
        isDestroying = true
        scope.cancel()
        val uptime = if (connectedAt > 0) (System.currentTimeMillis() - connectedAt) / 1000 else -1
        Log.w("AutoBookkeeper", "💀 [NLS] onDestroy uptime=${uptime}s")
        super.onDestroy()
        App.notificationListenerRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w("AutoBookkeeper", "🗑️ [NLS] onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() {
        Log.w("AutoBookkeeper", "⚠️ [NLS] onLowMemory")
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        val levelName = when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
            android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            else -> "UNKNOWN($level)"
        }
        Log.w("AutoBookkeeper", "⚠️ [NLS] onTrimMemory level=$levelName")
        super.onTrimMemory(level)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        sbn?.let { handle(it) }
    }

    private fun handle(sbn: StatusBarNotification) {
        // 主线程: 记录健康时间戳 + 提取基础字段
        getSharedPreferences("nls_health", MODE_PRIVATE)
            .edit().putLong("last_notification_time", System.currentTimeMillis()).apply()

        val n = sbn.notification ?: return
        val pkg = sbn.packageName
        val title = n.extras?.getString(Notification.EXTRA_TITLE) ?: ""
        val text = getAllText(n)

        // 解析 (regex) 挪到 IO 协程, 不阻塞主线程
        scope.launch {
            try {
                if (!parser.isPaymentNotification(pkg, title, text)) return@launch

                val notificationId = "nls_" + sbn.key
                val payment = parser.parsePayment(pkg, title, text, notificationId)
                if (payment == null) {
                    Log.w("AutoBookkeeper", "⚠️ parsePayment 返回 null! pkg=$pkg title=$title text前50字=${text.take(50)}")
                    return@launch
                }

                Log.d("AutoBookkeeper", "📝 parse成功: ${payment.platform}/${payment.merchant}/¥${payment.amount} notificationId=$notificationId")

                when (val result = expenseRepository.insertRaw(payment)) {
                    is InsertResult.Inserted -> {
                        Log.i("AutoBookkeeper", "💰 ${payment.platform}: ${payment.merchant} ¥${payment.amount}")
                    }
                    is InsertResult.Updated -> {
                        Log.i(
                            "AutoBookkeeper",
                            "🔄 ${payment.platform}: ${result.existing.merchant}→${payment.merchant} ¥${payment.amount} (更新字段: ${result.updatedFields.joinToString()})"
                        )
                    }
                    is InsertResult.Duplicate -> {
                        val ex = result.existing
                        Log.d(
                            "AutoBookkeeper",
                            "⏭ 重复: notificationId=$notificationId | 已在DB id=${ex.id} merchant=${ex.merchant} amount=${ex.amount} recordedAt=${ex.recordedAt}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoBookkeeper", "handle 协程异常: $pkg/$title", e)
            }
        }
    }

    private fun startForegroundNotification() {
        if (isDestroying) {
            Log.w("AutoBookkeeper", "⏭ [NLS] startForeground skipped: service is destroying")
            return
        }
        try {
            val ch = NotificationChannel("nls", "监听", NotificationManager.IMPORTANCE_LOW).apply {
                description = "保持通知监听"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
            val n = NotificationCompat.Builder(this, "nls")
                .setContentTitle("自动记账")
                .setContentText("通知监听运行中")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1001,
                    n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(1001, n)
            }
        } catch (e: Exception) {
            Log.e("AutoBookkeeper", "前台通知启动失败, 服务可能被系统杀死", e)
        }
    }

    private fun getAllText(n: Notification): String {
        val e = n.extras ?: return ""
        val p = mutableListOf<String>()

        val lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty()) {
            p.add(lines.joinToString(" ") { it.toString().trim() })
        }
        e.getString(Notification.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { p.add(it) }
        e.getString(Notification.EXTRA_BIG_TEXT)?.takeIf { it.isNotBlank() }?.let { p.add(it) }
        e.getString(Notification.EXTRA_SUMMARY_TEXT)?.takeIf { it.isNotBlank() }?.let { p.add(it) }
        e.getString(Notification.EXTRA_SUB_TEXT)?.takeIf { it.isNotBlank() }?.let { p.add(it) }
        n.tickerText?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { p.add(it) }
        return p.distinct().joinToString("\n").trim()
    }
}
