package com.example.autobookkeeper.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.autobookkeeper.data.repository.ExpenseRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    @Inject
    lateinit var paymentParser: PaymentParser

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            serviceScope.launch {
                processNotification(it)
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val packageName = sbn.packageName
        val notificationId = "${sbn.packageName}_${sbn.id}_${sbn.postTime}"

        val title = getTitle(notification)
        val allText = getAllText(notification)

        Log.d("AutoBookkeeper", "=== 收到通知 ===")
        Log.d("AutoBookkeeper", "包名: $packageName")
        Log.d("AutoBookkeeper", "标题: $title")
        Log.d("AutoBookkeeper", "内容: $allText")

        if (paymentParser.isPaymentNotification(packageName, title, allText)) {
            Log.d("AutoBookkeeper", ">>> 识别为支付通知，开始解析")
            val expense = paymentParser.parsePayment(packageName, title, allText, notificationId)
            if (expense != null) {
                val safeExpense = expense.copy(
                    category = expense.category.ifBlank { "其他" }
                )
                expenseRepository.insertExpense(safeExpense)
                Log.d("AutoBookkeeper", ">>> 记账成功: ${expense.platform} ¥${expense.amount} ${expense.merchant}")
            } else {
                Log.d("AutoBookkeeper", ">>> 解析失败：无法提取金额")
            }
        }
    }

    private fun getTitle(notification: Notification): String {
        return notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
    }

    private fun getAllText(notification: Notification): String {
        val extras = notification.extras
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        val sb = StringBuilder()
        if (text.isNotEmpty()) sb.appendLine(text)
        if (bigText.isNotEmpty()) sb.appendLine(bigText)
        if (summaryText.isNotEmpty()) sb.appendLine(summaryText)
        if (subText.isNotEmpty()) sb.appendLine(subText)
        lines?.forEach { line ->
            if (line.isNotEmpty()) sb.appendLine(line.toString())
        }
        return sb.toString().trim()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("AutoBookkeeper", "通知监听服务已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("AutoBookkeeper", "通知监听服务已断开")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}