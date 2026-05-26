package com.example.autobookkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isSameDay(today: Calendar, target: Calendar): Boolean {
    return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(today: Calendar, target: Calendar): Boolean {
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(yesterday, target)
}

fun formatGroupDate(dateMillis: Long): String {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
    val prefix = sdf.format(Date(dateMillis))
    return when {
        isSameDay(today, target) -> "$prefix 今天"
        isYesterday(today, target) -> "$prefix 昨天"
        else -> {
            val dayNames = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
            "$prefix ${dayNames[target.get(Calendar.DAY_OF_WEEK) - 1]}"
        }
    }
}

@Composable
fun DayGroupHeader(
    dateMillis: Long,
    totalOut: Double,
    totalIn: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatGroupDate(dateMillis),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (totalOut > 0) {
                Text(
                    text = "出 ¥${"%.2f".format(totalOut)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (totalIn > 0) {
                Text(
                    text = "入 ¥${"%.2f".format(totalIn)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}