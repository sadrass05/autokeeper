package com.example.autobookkeeper.ui.importdata

data class ImportResult(
    val successCount: Int = 0,
    val skipCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList()
)