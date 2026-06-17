package com.example.autobookkeeper.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autobookkeeper.App

/**
 * BOOT_COMPLETED / MY_PACKAGE_REPLACED 接收器
 *
 * 设备重启 / APK 升级后, 通知监听 service 不会自动 rebind, 需要显式触发
 *
 * 触发后:
 * 1. 调度 NlsWatchdogWorker (持续健康检查)
 * 2. 调度 NlsAlarmReceiver (AlarmManager 兜底)
 * 3. 如果 NLS 已死, 调 kickOnce 立即恢复
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("AutoBookkeeper", "🔄 BootReceiver: action=$action")

        val app = context.applicationContext as? App ?: return
        if (!app.isNotificationListenerEnabled()) {
            Log.w("AutoBookkeeper", "通知权限未授予, 只调度 worker 不调 kickOnce")
            NlsWatchdogWorker.schedule(context)
            NlsAlarmReceiver.schedule(context)
            return
        }

        // 权限在, 全部调度
        NlsWatchdogWorker.schedule(context)
        NlsAlarmReceiver.schedule(context)

        if (!App.notificationListenerRunning) {
            Log.w("AutoBookkeeper", "⚠️ 设备重启后 NLS 未自动 rebind, 立即 kick")
            NlsRestartWorker.kickOnce(context)
        }
    }
}
