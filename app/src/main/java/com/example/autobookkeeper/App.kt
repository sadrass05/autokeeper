package com.example.autobookkeeper

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.autobookkeeper.backup.BackupManager
import com.example.autobookkeeper.backup.VerifyResult
import com.example.autobookkeeper.backup.WeeklyBackupWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class App : Application() {

    @Inject @Singleton
    lateinit var backupManager: BackupManager

    override fun onCreate() {
        super.onCreate()
        
        WeeklyBackupWorker.schedule(this)
        
        (this as ProcessLifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
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
    }

    companion object {
        private const val TAG = "App"
    }
}