package com.example.autobookkeeper.backup

import android.content.Context
import android.os.Environment
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.repository.ExpenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val fileName: String, val count: Int) : BackupResult()
    data class Failure(val error: String) : BackupResult()
}

sealed class VerifyResult {
    object Consistent : VerifyResult()
    object NoBackup : VerifyResult()
    data class Inconsistent(val appCount: Int, val backupCount: Int, val suggestion: String) : VerifyResult()
    data class Failure(val error: String) : VerifyResult()
}

sealed class RestoreResult {
    data class Success(val restoredCount: Int) : RestoreResult()
    data class Failure(val error: String) : RestoreResult()
}

data class BackupFile(
    val file: File,
    val name: String,
    val size: Long,
    val date: Date,
    val recordCount: Int = 0
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository
) {
    private val backupDir by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups/weekly")
            .apply { mkdirs() }
    }

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun performWeeklyBackup(): BackupResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val expenses = expenseRepository.getAllExpensesOnce()
                .filter { !it.isDeleted }

            if (expenses.isEmpty()) {
                return@withContext BackupResult.Failure("无数据可备份")
            }

            val dateStr = dateFormat.format(Date())
            val fileName = "backup_$dateStr.csv"
            val file = File(backupDir, fileName)

            writeCsv(file, expenses)
            cleanOldBackups()

            BackupResult.Success(fileName, expenses.size)
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult.Failure(e.message ?: "备份失败")
        }
    }

    suspend fun performManualBackup(): BackupResult = performWeeklyBackup()

    private fun writeCsv(file: File, expenses: List<ExpenseRecord>) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出")

        expenses.forEach { e ->
            sb.appendLine(listOf(
                csvDateFormat.format(Date(e.recordedAt)),
                e.merchant.ifBlank { "未知商户" },
                "%.2f".format(e.amount),
                e.platform.ifBlank { "未知平台" },
                e.paymentChannel.ifBlank { "未知" },
                e.category.ifBlank { "未分类" },
                if (e.isFinanceExpense) "是" else "否"
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    fun getBackupList(): List<BackupFile> {
        return (backupDir.listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".csv") && it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                BackupFile(
                    file = file,
                    name = file.name,
                    size = file.length(),
                    date = Date(file.lastModified()),
                    recordCount = countRecordsInCsv(file)
                )
            }
    }

    private fun countRecordsInCsv(file: File): Int {
        return try {
            file.useLines { lines -> lines.count() - 1 }
        } catch (_: Exception) {
            0
        }
    }

    private fun cleanOldBackups() {
        val twoWeeksAgo = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        (backupDir.listFiles() ?: emptyArray()).forEach { file ->
            if (file.lastModified() < twoWeeksAgo && file.name.endsWith(".csv")) {
                file.delete()
            }
        }
    }

    suspend fun verifyDataIntegrity(): VerifyResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val latestBackup = getBackupList().firstOrNull()?.file
                ?: return@withContext VerifyResult.NoBackup

            val backupDate = parseBackupDate(latestBackup.name)
            val appDataBeforeBackup = expenseRepository.getExpensesBeforeTime(backupDate).first()
                .filter { !it.isDeleted }

            val backupData = parseCsvToExpenses(latestBackup)

            val appCount = appDataBeforeBackup.size
            val backupCount = backupData.size

            when {
                appCount == backupCount -> VerifyResult.Consistent
                appCount < backupCount -> VerifyResult.Inconsistent(
                    appCount = appCount,
                    backupCount = backupCount,
                    suggestion = if (appCount == 0)
                        "检测到数据可能被清空，建议从备份恢复"
                    else
                        "检测到部分数据缺失，可能是手动删除或异常，是否从备份恢复？"
                )
                else -> VerifyResult.Consistent // App数据更多（新增了数据），正常
            }
        } catch (e: Exception) {
            e.printStackTrace()
            VerifyResult.Failure(e.message ?: "校验失败")
        } as? VerifyResult ?: VerifyResult.Failure("校验异常")
    }

    suspend fun restoreFromBackup(backupFile: File): RestoreResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val backupData = parseCsvToExpenses(backupFile)
            val backupDate = parseBackupDate(backupFile.name)

            val existingIds = expenseRepository.getExpensesBeforeTime(backupDate).first()
                .map { it.id }.toSet()

            var restored = 0
            backupData.forEach { expense ->
                if (!existingIds.contains(expense.id)) {
                    expenseRepository.insertExpense(expense)
                    restored++
                }
            }

            RestoreResult.Success(restored)
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult.Failure(e.message ?: "恢复失败")
        }
    }

    fun deleteBackup(file: File): Boolean {
        return try {
            if (file.exists() && file.parentFile?.absolutePath == backupDir.absolutePath) {
                file.delete()
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun parseBackupDate(fileName: String): Long {
        return try {
            val dateStr = fileName.removePrefix("backup_").removeSuffix(".csv")
            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseCsvToExpenses(file: File): List<ExpenseRecord> {
        return try {
            val lines = file.readText(Charsets.UTF_8).lines().drop(1).filter { it.isNotBlank() }
            lines.mapNotNull { line ->
                runCatching {
                    val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (cols.size >= 7) {
                        ExpenseRecord(
                            merchant = cols[1].ifBlank { "未知商户" },
                            amount = cols[2].trim().replace("¥", "").toDoubleOrNull() ?: 0.0,
                            platform = cols[3].ifBlank { "未知平台" },
                            paymentChannel = cols[4].ifBlank { "未知" },
                            category = cols[5].ifBlank { "未分类" },
                            isFinanceExpense = cols[6].contains("是"),
                            recordedAt = parseDateTime(cols[0]),
                            notificationId = "restored_${System.currentTimeMillis()}",
                            isDeleted = false,
                            deletedAt = null
                        )
                    } else null
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDateTime(dateStr: String): Long {
        val cleaned = dateStr.trim().removeSurrounding("\"")
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "日期时间") {
            return System.currentTimeMillis()
        }
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "MM-dd HH:mm"
        )
        formats.forEach { pattern ->
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(cleaned)?.time ?: return@runCatching
            }
        }
        return System.currentTimeMillis()
    }
}