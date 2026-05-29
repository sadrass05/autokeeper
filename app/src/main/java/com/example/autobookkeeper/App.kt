package com.example.autobookkeeper

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.autobookkeeper.backup.BackupManager
import com.example.autobookkeeper.backup.VerifyResult
import com.example.autobookkeeper.backup.WeeklyBackupWorker
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
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

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
    }
}
