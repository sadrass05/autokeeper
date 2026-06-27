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
import java.util.Date
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
    /**
     * 测试用 secondary constructor: 直接注入 backupDir 路径, 避免在 JVM 单测中 mock Context.
     * 产线代码仍走 primary constructor (从 context.getExternalFilesDir 派生路径).
     */
    internal constructor(
        context: Context,
        expenseRepository: ExpenseRepository,
        backupDir: File
    ) : this(context, expenseRepository) {
        this.backupDirOverride = backupDir
    }

    private var backupDirOverride: File? = null

    /** 测试访问器: 返回注入的 backupDir 路径, 产线代码不应调用. */
    internal fun backupDirOverrideForTest(): File? = backupDirOverride

    /** 测试访问器: 调用 writeCsv 供测试使用. */
    internal fun writeCsvForTest(file: File, expenses: List<ExpenseRecord>) {
        writeCsv(file, expenses)
    }

    /** 测试访问器: 调用 cleanOldBackupsImpl 供测试使用. */
    internal fun cleanOldBackupsImplForTest(dir: File, keep: Int) {
        cleanOldBackupsImpl(dir, keep)
    }

    private val backupDir: File by lazy {
        backupDirOverride
            ?: File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups/weekly")
                .apply { mkdirs() }
    }

    private val dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
    private val csvDateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    suspend fun performWeeklyBackup(): BackupResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val expenses = expenseRepository.getAllExpensesOnce()
                .filter { !it.isDeleted }

            if (expenses.isEmpty()) {
                return@withContext BackupResult.Success(fileName = "", count = 0)
            }

            val dateStr = java.time.LocalDate.now().format(dateFormat)
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

    internal fun writeCsv(file: File, expenses: List<ExpenseRecord>) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出")

        expenses.forEach { e ->
            val fields = listOf(
                csvDateFormat.format(
                    java.time.Instant.ofEpochMilli(e.recordedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                ),
                e.merchant.ifBlank { "未知商户" },
                "%.2f".format(e.amount),
                e.platform.ifBlank { "未知平台" },
                e.paymentChannel.ifBlank { "未知" },
                e.category.ifBlank { "未分类" },
                if (e.isFinanceExpense) "是" else "否"
            )
            sb.appendLine(fields.joinToString(",") { escapeCsvField(it) })
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    /**
     * RFC 4180 CSV 字段转义:
     * - 字段含 `,` / `"` / `\n` / `\r` → 用双引号包围
     * - 字段内的 `"` → 转义为 `""`
     * - 普通字段不加引号 (兼容旧解析器)
     */
    internal fun escapeCsvField(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
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

    private fun cleanOldBackups() = cleanOldBackupsImpl(backupDir, keep = MAX_BACKUP_FILES)

    /**
     * 清理老备份, 保留最近 keep 个文件.
     * 修复前用 14 天硬编码清理窗口, 加上 7 天备份周期, 实际可能只剩 1 个.
     * 修复后按数量保留, 与备份周期解耦.
     */
    internal fun cleanOldBackupsImpl(dir: File, keep: Int) {
        val files = (dir.listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".csv") && it.isFile }
            .sortedByDescending { it.lastModified() }
        if (files.size > keep) {
            files.drop(keep).forEach { it.delete() }
        }
    }

    private companion object {
        /** 保留最近 4 个备份 (在 7 天备份周期下, 覆盖 1 个月数据). */
        const val MAX_BACKUP_FILES = 4
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
            if (backupData.isEmpty()) return@withContext RestoreResult.Success(0)

            val existingRecords = expenseRepository.getExpensesBeforeTime(
                System.currentTimeMillis()
            ).first()

            val existingSignatures = existingRecords
                .associateBy { buildSignature(it) }

            val toInsert = backupData.filter { record ->
                !existingSignatures.containsKey(buildSignature(record))
            }

            if (toInsert.isNotEmpty()) {
                expenseRepository.insertExpensesBatch(toInsert)
            }

            RestoreResult.Success(toInsert.size)
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
            java.time.LocalDate.parse(dateStr, dateFormat)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    internal fun parseCsvToExpenses(file: File): List<ExpenseRecord> {
        return try {
            val lines = file.readText(Charsets.UTF_8).lines()
                .drop(1)  // 跳过表头
                .filter { it.isNotBlank() }
            lines.mapIndexedNotNull { index, line ->
                runCatching {
                    val cols = parseCsvLine(line)
                    if (cols.size >= 7) {
                        ExpenseRecord(
                            merchant = cols[1].ifBlank { "未知商户" },
                            amount = cols[2].trim().replace("¥", "").toDoubleOrNull() ?: 0.0,
                            platform = cols[3].ifBlank { "未知平台" },
                            paymentChannel = cols[4].ifBlank { "未知" },
                            category = cols[5].ifBlank { "未分类" },
                            isFinanceExpense = cols[6].contains("是"),
                            recordedAt = parseDateTime(cols[0]),
                            notificationId = "restored_${System.nanoTime()}_$index",
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

    /**
     * CSV 单行解析, 支持 RFC 4180 双引号转义:
     * - 字段含逗号 → 用双引号包围
     * - 字段内含双引号 → 转义为两个双引号 ""
     * - 行尾 `\r\n` 由 readText().lines() 自动去除
     */
    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    // 双引号转义: "" → "
                    current.append('"')
                    i += 2
                    continue
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun parseDateTime(dateStr: String): Long {
        val cleaned = dateStr.trim().removeSurrounding("\"")
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "日期时间") {
            return System.currentTimeMillis()
        }
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.LocalDate.now()
        val formattersWithYear = listOf(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        )
        for (f in formattersWithYear) {
            runCatching {
                val ldt = java.time.LocalDateTime.parse(cleaned, f)
                return ldt.atZone(zone).toInstant().toEpochMilli()
            }
        }
        runCatching {
            val f = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
            val md = java.time.MonthDay.parse(cleaned, f)
            return md.atYear(now.year).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        return System.currentTimeMillis()
    }

    private fun buildSignature(record: ExpenseRecord): String {
        val normalizedAmount = "%.2f".format(record.amount)
        val normalizedTime = (record.recordedAt / 60000) * 60000
        return "${record.merchant}|$normalizedAmount|$normalizedTime"
    }
}