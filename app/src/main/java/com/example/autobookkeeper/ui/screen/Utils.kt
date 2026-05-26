package com.example.autobookkeeper.ui.screen

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
}