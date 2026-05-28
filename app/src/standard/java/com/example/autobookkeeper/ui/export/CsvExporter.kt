package com.example.autobookkeeper.ui.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.autobookkeeper.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {

    private fun buildCsv(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine(headers.joinToString(",") {
            "\"${it.replace("\"", "\"\"")}\""
        })
        rows.forEach { row ->
            sb.appendLine(row.joinToString(",") {
                "\"${it.replace("\"", "\"\"")}\""
            })
        }
        return sb.toString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveToDownloads(
        context: Context,
        fileName: String,
        content: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IOException("创建文件失败")
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }
    }

    suspend fun exportExpenses(context: Context): ExportResult {
        val dateStr = SimpleDateFormat(
            "yyyyMMdd_HHmm", Locale.getDefault()
        ).format(Date())
        val fileName = "expense_records_$dateStr.csv"
        val expenses = expenseRepository.getAllExpensesOnce()
        val headers = listOf(
            "日期时间", "商户名称", "金额",
            "平台", "支付渠道", "分类", "是否理财支出"
        )
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val rows = expenses.map { e ->
            listOf(
                dateFormat.format(Date(e.recordedAt)),
                e.merchant.ifBlank { "未知商户" },
                "%.2f".format(e.amount),
                e.platform.ifBlank { "未知平台" },
                e.paymentChannel.ifBlank { "未知" },
                e.category.ifBlank { "未分类" },
                if (e.isFinanceExpense) "是" else "否"
            )
        }
        val csv = buildCsv(headers, rows)
        return saveToDownloads(context, fileName, csv).fold(
            onSuccess = { uri ->
                ExportResult.Success(fileName, uri, expenses.size)
            },
            onFailure = { e ->
                ExportResult.Failure(e.message ?: "未知错误")
            }
        )
    }

    suspend fun exportPositions(context: Context): ExportResult {
        return ExportResult.Failure("标准版不支持导出理财持仓")
    }
}