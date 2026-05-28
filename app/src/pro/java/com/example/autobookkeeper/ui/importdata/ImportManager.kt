package com.example.autobookkeeper.ui.importdata

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.data.repository.FinanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.text.Charsets

@Singleton
class ImportManager @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val financeRepository: FinanceRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun importFile(uri: Uri): ImportResult {
        val fileName = getFileName(uri)
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return when {
            fileName.endsWith(".csv", ignoreCase = true)
                || mimeType.contains("csv") -> importCsvAutoDetect(uri)
            fileName.endsWith(".txt", ignoreCase = true)
                || mimeType.contains("text") -> importTxtAutoDetect(uri)
            else -> ImportResult(
                errorCount = 1,
                errors = listOf("不支持的文件格式，请使用 CSV 或 TXT 文件")
            )
        }
    }

    private suspend fun importCsvAutoDetect(uri: Uri): ImportResult {
        val firstLine = readFirstLine(uri) ?: ""
        return when {
            firstLine.contains("产品名称") -> importFinanceCsv(uri)
            firstLine.contains("商户名称") || firstLine.contains("日期时间") -> importExpensesCsv(uri)
            else -> ImportResult(
                errorCount = 1,
                errors = listOf("无法识别的CSV格式，表头应包含「日期时间,商户名称,...」或「产品名称,平台,...」")
            )
        }
    }

    private suspend fun importTxtAutoDetect(uri: Uri): ImportResult {
        val content = readContent(uri)
        return when {
            content.contains("商户名称:") -> importExpensesTxt(content)
            content.contains("产品名称:") -> importFinanceTxt(content)
            else -> ImportResult(errorCount = 1, errors = listOf("无法识别的TXT格式"))
        }
    }

    private suspend fun importExpensesCsv(uri: Uri): ImportResult {
        val lines = readLines(uri)
        if (lines.isEmpty()) return ImportResult(errorCount = 1, errors = listOf("文件为空"))
        val dataLines = lines.drop(1)
        val errors = mutableListOf<String>()
        var success = 0
        dataLines.forEachIndexed { index, line ->
            runCatching {
                val cols = parseCsvLine(line)
                if (cols.size < 7) {
                    errors.add("第${index + 2}行：列数不足 (需要7列，实际${cols.size}列)")
                    return@forEachIndexed
                }
                val amount = cols[2].trim().replace("\"", "")
                    .replace("¥", "").replace("￥", "")
                    .toDoubleOrNull() ?: 0.0
                val record = ExpenseRecord(
                    merchant = cols.getOrNull(1)?.trim()?.replace("\"", "")
                        ?.ifBlank { "未知商户" } ?: "未知商户",
                    amount = amount,
                    platform = cols.getOrNull(3)?.trim()?.replace("\"", "")
                        ?.ifBlank { "未知平台" } ?: "未知平台",
                    paymentChannel = cols.getOrNull(4)?.trim()?.replace("\"", "")
                        ?.ifBlank { "未知" } ?: "未知",
                    category = cols.getOrNull(5)?.trim()?.replace("\"", "")
                        ?.ifBlank { "未分类" } ?: "未分类",
                    isFinanceExpense = cols.getOrNull(6)?.trim()?.replace("\"", "")?.contains("是") == true,
                    recordedAt = parseDateTime(cols[0]),
                    notificationId = "import_csv_${System.currentTimeMillis()}_${index + 2}"
                )
                if (record.merchant.isNotBlank() && record.amount > 0) {
                    expenseRepository.insertExpense(record)
                    success++
                } else {
                    errors.add("第${index + 2}行：商户名称为空或金额无效")
                }
            }.onFailure {
                errors.add("第${index + 2}行：${it.message}")
            }
        }
        return ImportResult(successCount = success, errorCount = errors.size, errors = errors)
    }

    private suspend fun importExpensesTxt(content: String): ImportResult {
        val blocks = content.split(Regex("\\n\\s*\\n"))
        val errors = mutableListOf<String>()
        var success = 0
        blocks.forEachIndexed { blockIndex, block ->
            if (block.isBlank()) return@forEachIndexed
            runCatching {
                val map = block.lines()
                    .filter { it.contains(":") }
                    .associate { line ->
                        val idx = line.indexOf(":")
                        line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                    }
                val amount = map["金额"]
                    ?.replace("¥", "")?.replace("￥", "")
                    ?.toDoubleOrNull() ?: 0.0
                val record = ExpenseRecord(
                    merchant = map["商户名称"]
                        ?.ifBlank { "未知商户" } ?: "未知商户",
                    amount = amount,
                    platform = map["平台"]
                        ?.ifBlank { "未知平台" } ?: "未知平台",
                    paymentChannel = map["支付渠道"]
                        ?.ifBlank { "未知" } ?: "未知",
                    category = map["分类"]
                        ?.ifBlank { "未分类" } ?: "未分类",
                    isFinanceExpense = (map["是否理财支出"] ?: "").contains("是"),
                    recordedAt = parseDateTime(map["日期时间"] ?: map["日期"] ?: ""),
                    notificationId = "import_txt_${System.currentTimeMillis()}_$blockIndex"
                )
                if (record.merchant.isNotBlank() && record.amount > 0) {
                    expenseRepository.insertExpense(record)
                    success++
                } else {
                    errors.add("第${blockIndex + 1}块：商户名称为空或金额无效")
                }
            }.onFailure {
                errors.add("第${blockIndex + 1}块：${it.message}")
            }
        }
        return ImportResult(successCount = success, errorCount = errors.size, errors = errors)
    }

    private suspend fun importFinanceCsv(uri: Uri): ImportResult {
        val lines = readLines(uri)
        if (lines.isEmpty()) return ImportResult(errorCount = 1, errors = listOf("文件为空"))
        val dataLines = lines.drop(1)
        val errors = mutableListOf<String>()
        var success = 0
        dataLines.forEachIndexed { index, line ->
            runCatching {
                val cols = parseCsvLine(line)
                if (cols.size >= 6) {
                    val position = FinancePosition(
                        productName = cols[0].trim().replace("\"", ""),
                        platform = cols[1].trim().replace("\"", ""),
                        buyAmount = cols[2].trim().replace("\"", "")
                            .replace("¥", "").toDoubleOrNull() ?: 0.0,
                        currentValue = cols[3].trim().replace("\"", "")
                            .replace("¥", "").toDoubleOrNull() ?: 0.0,
                        profit = cols[4].trim().replace("\"", "")
                            .replace("¥", "").toDoubleOrNull() ?: 0.0,
                        profitRate = cols[5].trim().replace("\"", "")
                            .replace("%", "").toDoubleOrNull() ?: 0.0,
                        screenshotPath = "",
                        updatedAt = System.currentTimeMillis()
                    )
                    if (position.productName.isNotBlank()) {
                        financeRepository.insertPosition(position)
                        success++
                    } else {
                        errors.add("第${index + 2}行：产品名称为空")
                    }
                } else {
                    errors.add("第${index + 2}行：列数不足 (需要6列，实际${cols.size}列)")
                }
            }.onFailure {
                errors.add("第${index + 2}行：${it.message}")
            }
        }
        return ImportResult(successCount = success, errorCount = errors.size, errors = errors)
    }

    private suspend fun importFinanceTxt(content: String): ImportResult {
        val blocks = content.split(Regex("\\n\\s*\\n"))
        val errors = mutableListOf<String>()
        var success = 0
        blocks.forEachIndexed { blockIndex, block ->
            if (block.isBlank()) return@forEachIndexed
            runCatching {
                val map = block.lines()
                    .filter { it.contains(":") }
                    .associate { line ->
                        val idx = line.indexOf(":")
                        line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                    }
                val position = FinancePosition(
                    productName = map["产品名称"] ?: "",
                    platform = map["平台"] ?: "",
                    buyAmount = map["买入金额"]
                        ?.replace("¥", "")?.toDoubleOrNull() ?: 0.0,
                    currentValue = map["当前市值"]
                        ?.replace("¥", "")?.toDoubleOrNull() ?: 0.0,
                    profit = map["收益"]
                        ?.replace("¥", "")?.toDoubleOrNull() ?: 0.0,
                    profitRate = map["收益率"]
                        ?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                    screenshotPath = "",
                    updatedAt = System.currentTimeMillis()
                )
                if (position.productName.isNotBlank()) {
                    financeRepository.insertPosition(position)
                    success++
                } else {
                    errors.add("第${blockIndex + 1}块：产品名称为空")
                }
            }.onFailure {
                errors.add("第${blockIndex + 1}块：${it.message}")
            }
        }
        return ImportResult(successCount = success, errorCount = errors.size, errors = errors)
    }

    private fun parseDateTime(dateStr: String): Long {
        val cleaned = dateStr.trim().replace("\"", "")
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "日期时间") {
            Log.w("ImportManager", "空日期字段，使用当前时间")
            return System.currentTimeMillis()
        }
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "MM-dd HH:mm",
            "yyyy年MM月dd日 HH:mm"
        )
        formats.forEach { pattern ->
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(cleaned)?.time ?: return@runCatching
            }
        }
        Log.w("ImportManager", "无法解析日期: '$dateStr'，使用当前时间")
        return System.currentTimeMillis()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun readLines(uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.readLines()
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readFirstLine(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun readContent(uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.readText()
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }
}