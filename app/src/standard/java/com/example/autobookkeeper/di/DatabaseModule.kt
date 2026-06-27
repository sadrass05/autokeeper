package com.example.autobookkeeper.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.autobookkeeper.data.AppDatabase
import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val dbName = "autobookkeeper_database"
        val oldDbName = "autobookkeeper.db"

        migrateOldDatabaseIfNeeded(context, oldDbName, dbName)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()
}

private fun migrateOldDatabaseIfNeeded(context: Context, oldName: String, newName: String) {
    val dbDir = context.getDatabasePath(oldName).parentFile ?: return
    val oldDbFile = File(dbDir, oldName)
    val newDbFile = File(dbDir, newName)

    val shouldMigrate = oldDbFile.exists() && (
        !newDbFile.exists() || newDbFile.length() == 0L
    )

    if (!shouldMigrate) return

    try {
        oldDbFile.copyTo(newDbFile, overwrite = true)
        arrayOf("$oldName-wal", "$oldName-shm").forEach { suffix ->
            val oldAux = File(dbDir, suffix)
            val newAux = File(dbDir, suffix.replace(oldName, newName))
            if (oldAux.exists()) oldAux.copyTo(newAux, overwrite = true)
        }
        Log.i("AutoBookkeeper", "✅ 数据库已从 $oldName 迁移到 $newName (${oldDbFile.length()} bytes)")
    } catch (e: Exception) {
        Log.e("AutoBookkeeper", "❌ 数据库迁移失败", e)
    }
}
