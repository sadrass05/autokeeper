package com.example.autobookkeeper.ui.export

sealed class ExportResult {
    data class Success(
        val fileName: String,
        val uri: android.net.Uri,
        val count: Int
    ) : ExportResult()

    data class Failure(
        val error: String
    ) : ExportResult()
}