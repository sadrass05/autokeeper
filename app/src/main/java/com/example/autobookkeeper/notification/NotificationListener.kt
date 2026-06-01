package com.example.autobookkeeper.notification

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.di.ExpenseRepoEntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    private val fallbackParser = PaymentParser()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastScanTime: Long = 0
    private val minScanIntervalMs = 5 * 60 * 1000L

    private val pendingNotifications = ConcurrentLinkedQueue<StatusBarNotification>()
    private var retryCount = 0
    private val maxRetryDelayMs = 5000L

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("AutoBookkeeper", "🔔 通知监听服务已连接")
        showToast("🔔 通知监听已连接")
        schedulePendingFlush()
        scanExistingNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("AutoBookkeeper", "⚠️ 通知监听服务已断开连接")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        Log.d("AutoBookkeeper", "📩 收到通知: pkg=${sbn.packageName}, id=${sbn.id}, tag=${sbn.tag}")
        val repo = getRepo()
        if (repo == null) {
            Log.w("AutoBookkeeper", "⚠️ Repository未就绪，暂存通知: ${sbn.packageName}")
            pendingNotifications.add(sbn)
            schedulePendingFlush()
            return
        }
        processNotification(sbn, repo)
    }

    private fun processNotification(sbn: StatusBarNotification, repo: ExpenseRepository? = null) {
        val notification = sbn.notification
        if (notification == null) {
            Log.d("AutoBookkeeper", "⏭ notification为null, pkg=${sbn.packageName}")
            return
        }
        val packageName = sbn.packageName
        val title = getTitle(notification)
        val allText = getAllText(notification)

        Log.d("AutoBookkeeper", "📄 title=[$title], textLen=${allText.length}")

        val parser = getParser()

        if (!parser.isPaymentNotification(packageName, title, allText)) return

        val notificationId = "ntf_${packageName}_${sbn.id}_${System.currentTimeMillis()}"
        val payment = parser.parsePayment(packageName, title, allText, notificationId) ?: return

        serviceScope.launch {
            try {
                val r = repo ?: getRepo()
                if (r != null) {
                    if (!r.existsByNotificationId(payment.notificationId)) {
                        r.insertExpense(payment)
                        Log.i("AutoBookkeeper", "✅ 已记录: ${payment.platform} ${payment.merchant} ¥${payment.amount}")
                        showToast("💰 ${payment.platform}: ${payment.merchant} ¥${payment.amount}")
                    } else {
                        Log.d("AutoBookkeeper", "⏭ 重复通知: ${payment.notificationId}")
                    }
                } else {
                    Log.w("AutoBookkeeper", "⚠️ Repository不可用，保存失败")
                }
            } catch (e: Exception) {
                Log.e("AutoBookkeeper", "保存支付记录失败", e)
            }
        }
    }

    private fun scanExistingNotifications() {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < minScanIntervalMs) {
            Log.d("AutoBookkeeper", "⏭ 距上次扫描不足5分钟, 跳过")
            return
        }
        lastScanTime = now

        serviceScope.launch {
            try {
                val notifications = getActiveNotifications()
                if (notifications == null || notifications.isEmpty()) {
                    Log.d("AutoBookkeeper", "📭 通知栏无已有通知")
                    return@launch
                }
                Log.i("AutoBookkeeper", "🔍 扫描 ${notifications.size} 条已有通知")
                var scanned = 0
                var recorded = 0
                var skipped = 0
                for (sbn in notifications) {
                    val n = sbn.notification ?: continue
                    val pkg = sbn.packageName
                    val title = getTitle(n)
                    val text = getAllText(n)
                    val parser = getParser()
                    if (parser.isPaymentNotification(pkg, title, text)) {
                        scanned++
                        val nid = "ntf_scan_${pkg}_${sbn.id}_${System.currentTimeMillis()}"
                        val payment = parser.parsePayment(pkg, title, text, nid)
                        if (payment != null) {
                            val r = getRepo()
                            if (r != null && !r.existsByNotificationId(payment.notificationId)) {
                                r.insertExpense(payment)
                                recorded++
                                Log.i("AutoBookkeeper", "✅ 扫描: ${payment.platform} ${payment.merchant} ¥${payment.amount}")
                            } else if (r != null) {
                                skipped++
                            }
                        }
                    }
                }
                Log.i("AutoBookkeeper", "📊 扫描: ${notifications.size}条, 支付$scanned, 录入$recorded, 跳过$skipped")
                if (recorded > 0) {
                    showToast("📊 扫描识别 $recorded 条支付记录")
                }
            } catch (e: SecurityException) {
                Log.w("AutoBookkeeper", "⚠️ 扫描失败（权限不足）")
                showToast("⚠️ 无扫描权限，请授权通知访问")
            } catch (e: Exception) {
                Log.w("AutoBookkeeper", "⚠️ 扫描异常", e)
            }
        }
    }

    private fun schedulePendingFlush() {
        if (pendingNotifications.isEmpty()) return
        val repo = getRepo()
        if (repo != null) {
            flushPendingNotifications(repo)
        } else {
            retryCount++
            val delay = minOf(500L * retryCount, maxRetryDelayMs)
            Log.d("AutoBookkeeper", "⏳ Repository未就绪, ${delay}ms后重试(第${retryCount}次)")
            mainHandler.postDelayed({
                schedulePendingFlush()
            }, delay)
        }
    }

    private fun flushPendingNotifications(repo: ExpenseRepository) {
        val pending = pendingNotifications.toList()
        pendingNotifications.clear()
        if (pending.isEmpty()) return
        Log.i("AutoBookkeeper", "📬 处理 ${pending.size} 条待处理通知")
        retryCount = 0
        pending.forEach { sbn ->
            processNotification(sbn, repo)
        }
    }

    private fun getParser(): PaymentParser = fallbackParser

    private fun getRepo(): ExpenseRepository? {
        if (::expenseRepository.isInitialized) {
            return expenseRepository
        }
        return try {
            val entryPoint = EntryPoints.get(applicationContext, ExpenseRepoEntryPoint::class.java)
            val repo = entryPoint.currentExpenseRepository()
            Log.i("AutoBookkeeper", "🔧 通过EntryPoints获取ExpenseRepository成功")
            repo
        } catch (e: Exception) {
            Log.w("AutoBookkeeper", "⚠️ EntryPoints获取Repository失败: ${e.message}")
            null
        }
    }

    private fun getTitle(notification: Notification): String {
        val extras = notification.extras
        return extras.getString(Notification.EXTRA_TITLE) ?: ""
    }

    private fun getAllText(notification: Notification): String {
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val titleBig = extras.getString(Notification.EXTRA_TITLE_BIG) ?: ""
        val infoText = extras.getString("android.extraInfoText") ?: ""

        val parts = mutableListOf<String>()

        if (titleBig.isNotBlank() && titleBig != title) parts.add(titleBig)

        val linesStr = lines?.joinToString(" ") { it.toString().trim() }?.trim() ?: ""
        if (linesStr.isNotBlank()) parts.add(linesStr)

        if (bigText.isNotBlank() && bigText != text) parts.add(bigText)

        if (text.isNotBlank()) parts.add(text)

        if (summaryText.isNotBlank()) parts.add(summaryText)
        if (subText.isNotBlank()) parts.add(subText)
        if (infoText.isNotBlank()) parts.add(infoText)

        notification.tickerText?.toString()?.trim()?.let {
            if (it.isNotBlank()) parts.add(it)
        }

        return parts.distinct().joinToString("\n").trim()
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
    }
}
