package com.example.autobookkeeper.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.autobookkeeper.App

/**
 * AlarmManager 兜底: 每 30 分钟一次, WorkManager 被电池优化限制时仍能唤醒
 *
 * 触发条件: 系统在 ELAPSED_REALTIME_WAKEUP 时回调, 检查 NLS 状态:
 * - 权限撤了 → 不再调度 (用户必须先去设置)
 * - NLS alive → 不做事, 但重新排下一次 alarm
 * - NLS 死了 → 调 kickOnce + 重新排下一次
 *
 * 调度方式: setExactAndAllowWhileIdle (Doze 模式下也能触发)
 */
class NlsAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "⏰ AlarmManager 唤醒, 检查 NLS 状态")
        val app = context.applicationContext as? App
        if (app == null) {
            Log.w(TAG, "App 实例为空, 跳过")
            return
        }

        if (!app.isNotificationListenerEnabled()) {
            Log.w(TAG, "通知权限未授予, 停止 alarm 调度")
            return
        }

        if (App.notificationListenerRunning) {
            Log.i(TAG, "NLS 仍 alive, 不做事")
        } else {
            Log.w(TAG, "⚠️ NLS 死了, 调 kickOnce")
            NlsRestartWorker.kickOnce(context)
        }

        // 重新排下一次 (30 分钟后)
        schedule(context)
    }

    companion object {
        private const val TAG = "NlsAlarmReceiver"
        private const val ALARM_INTERVAL_MS = 30 * 60 * 1000L  // 30 分钟

        fun schedule(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NlsAlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, 1002, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAt = SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                }
                Log.i(TAG, "⏰ 下次 alarm: ${ALARM_INTERVAL_MS / 60_000} 分钟后")
            } catch (e: Exception) {
                Log.e(TAG, "调度 alarm 失败", e)
            }
        }

        fun cancel(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NlsAlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, 1002, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
                Log.i(TAG, "⏰ alarm 已取消")
            } catch (e: Exception) {
                Log.e(TAG, "取消 alarm 失败", e)
            }
        }
    }
}
