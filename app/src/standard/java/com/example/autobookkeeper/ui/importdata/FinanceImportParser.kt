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

    suspend fun parse(context: Context, uri: Uri): List<ParsedPosition> = emptyList()
}
