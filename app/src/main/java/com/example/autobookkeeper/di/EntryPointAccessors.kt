package com.example.autobookkeeper.di

import android.content.Context
import com.example.autobookkeeper.backup.BackupManager
import com.example.autobookkeeper.data.SyncPrefs
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.network.SyncService
import com.example.autobookkeeper.ui.export.CsvExporter
import com.example.autobookkeeper.ui.importdata.ImportManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoints

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncPrefsEntryPoint {
    fun currentSyncPrefs(): SyncPrefs
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncServiceEntryPoint {
    fun currentSyncService(): SyncService
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CsvExporterEntryPoint {
    fun currentCsvExporter(): CsvExporter
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImportManagerEntryPoint {
    fun currentImportManager(): ImportManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpenseRepoEntryPoint {
    fun currentExpenseRepository(): ExpenseRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupManagerEntryPoint {
    fun currentBackupManager(): BackupManager
}

fun Context.currentSyncPrefs(): SyncPrefs =
    EntryPoints.get(this, SyncPrefsEntryPoint::class.java).currentSyncPrefs()

fun Context.currentSyncService(): SyncService =
    EntryPoints.get(this, SyncServiceEntryPoint::class.java).currentSyncService()

fun Context.currentCsvExporter(): CsvExporter =
    EntryPoints.get(this, CsvExporterEntryPoint::class.java).currentCsvExporter()

fun Context.currentImportManager(): ImportManager =
    EntryPoints.get(this, ImportManagerEntryPoint::class.java).currentImportManager()

fun Context.currentExpenseRepository(): ExpenseRepository =
    EntryPoints.get(this, ExpenseRepoEntryPoint::class.java).currentExpenseRepository()

fun Context.currentBackupManager(): BackupManager =
    EntryPoints.get(this, BackupManagerEntryPoint::class.java).currentBackupManager()
