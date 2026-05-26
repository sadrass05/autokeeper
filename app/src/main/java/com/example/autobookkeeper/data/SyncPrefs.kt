package com.example.autobookkeeper.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    var serverIp: String
        get() = prefs.getString("server_ip", "") ?: ""
        set(value) = prefs.edit().putString("server_ip", value).apply()

    var serverPort: Int
        get() = prefs.getInt("server_port", 5000)
        set(value) = prefs.edit().putInt("server_port", value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong("last_sync_time", 0L)
        set(value) = prefs.edit().putLong("last_sync_time", value).apply()

    fun getBaseUrl(): String {
        val ip = serverIp
        if (ip.isBlank()) return ""
        return "http://$ip:${serverPort}/"
    }
}