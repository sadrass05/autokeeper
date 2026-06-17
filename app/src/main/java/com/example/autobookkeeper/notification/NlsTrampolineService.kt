package com.example.autobookkeeper.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autobookkeeper.App

/**
 * 拉起服务：仅用于在本 app 进程被系统杀死后, 通过 startForegroundService
 * 把进程拉起, 让 NotificationManagerService 重新 bind NLS。
 *
 * 启动 1.5 秒后自动 stop self, 不长期占用 FGS 配额。
 */
class NlsTrampolineService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i("AutoBookkeeper", "🚀 NlsTrampolineService 启动, 尝试拉起 NLS 进程")
        try {
            val ch = NotificationChannel("trampoline", "服务拉起", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
            val n = NotificationCompat.Builder(this, "trampoline")
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(2001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(2001, n)
            }
        } catch (e: Exception) {
            Log.e("AutoBookkeeper", "Trampoline 启动 FGS 失败", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动后等 2.5 秒 (trampoline 拉起进程 2s + NLS 真正 bind 上 0.5s buffer)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val nlsAlive = App.notificationListenerRunning
                if (nlsAlive) {
                    Log.i("AutoBookkeeper", "✅ Trampoline 验证: NLS 已成功 bind, 正常退出")
                } else {
                    Log.w("AutoBookkeeper", "⚠️ Trampoline 验证: NLS 仍未 bind, requestRebind 也没生效, 等待下次 worker 周期")
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                Log.e("AutoBookkeeper", "Trampoline stopSelf 失败", e)
            }
        }, 2500)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
