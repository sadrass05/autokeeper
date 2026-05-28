package com.example.autobookkeeper.ui.importdata

import android.content.Context
import android.net.Uri

object FinanceImportParser {

    data class ParsedPosition(
        val productName: String,
        val platform: String,
        val buyAmount: Double,
        val currentValue: Double,
        val profit: Double,
        val profitRate: Double
    )

    suspend fun parse(context: Context, uri: Uri): List<ParsedPosition> {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readLines()
        } ?: return emptyList()

        val result = parseTable(lines)
        if (result.isNotEmpty()) return result
        return parseFreeText(lines)
    }

    private fun parseTable(lines: List<String>): List<ParsedPosition> {
        val result = mutableListOf<ParsedPosition>()
        for (line in lines) {
            val trimmed = line.trim()
            if (shouldSkipLine(trimmed)) continue
            val parts = if (trimmed.contains("\t")) trimmed.split("\t") else trimmed.split(",")
            val cols = parts.map { it.trim() }.filter { it.isNotEmpty() }
            if (cols.size <= 1) continue
            val productName = cols[0]
            val platform = if (cols.size > 1) cols[1] else "未知"
            val buyAmount = if (cols.size > 2) cols[2].toDoubleOrNull() ?: 0.0 else 0.0
            val currentValue = if (cols.size > 3) cols[3].toDoubleOrNull() ?: 0.0 else 0.0
            val profit = if (cols.size > 4) cols[4].toDoubleOrNull() ?: 0.0 else 0.0
            val profitRate = if (cols.size > 5) cols[5].toDoubleOrNull() ?: 0.0 else 0.0
            result.add(
                ParsedPosition(
                    productName = productName,
                    platform = platform,
                    buyAmount = buyAmount,
                    currentValue = currentValue,
                    profit = profit,
                    profitRate = profitRate
                )
            )
        }
        return result
    }

    private fun parseFreeText(lines: List<String>): List<ParsedPosition> {
        val result = mutableListOf<ParsedPosition>()
        val numberRegex = Regex("(\\d+\\.?\\d*)")
        for (line in lines) {
            val trimmed = line.trim()
            if (shouldSkipLine(trimmed)) continue
            val numbers = numberRegex.findAll(trimmed).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
            if (numbers.size < 2) continue
            val firstNumberIndex = trimmed.indexOfFirst { it.isDigit() }
            val productName = if (firstNumberIndex > 0) trimmed.substring(0, firstNumberIndex).trim() else trimmed
            result.add(
                ParsedPosition(
                    productName = productName,
                    platform = "未知",
                    buyAmount = numbers.getOrElse(0) { 0.0 },
                    currentValue = numbers.getOrElse(1) { 0.0 },
                    profit = numbers.getOrElse(2) { 0.0 },
                    profitRate = numbers.getOrElse(3) { 0.0 }
                )
            )
        }
        return result
    }

    private fun shouldSkipLine(line: String): Boolean {
        if (line.isEmpty()) return true
        val lower = line.lowercase()
        return lower.contains("产品") || lower.contains("名称") || lower.contains("日期")
    }
}
