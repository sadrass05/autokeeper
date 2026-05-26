package com.example.autobookkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autobookkeeper.data.entity.ExpenseRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val categoryColorMap = mapOf(
    "餐饮" to Color(0xFFF48FB1),
    "购物" to Color(0xFF80CBC4),
    "交通" to Color(0xFF90CAF9),
    "娱乐" to Color(0xFFCE93D8),
    "日用" to Color(0xFFA5D6A7),
    "医疗" to Color(0xFFEF9A9A),
    "教育" to Color(0xFFFFE082),
    "通讯" to Color(0xFFB39DDB),
    "居住" to Color(0xFFFFAB91),
    "其他" to Color(0xFFB0BEC5)
)

private val fallbackColors = listOf(
    Color(0xFFF48FB1),
    Color(0xFF80CBC4),
    Color(0xFF90CAF9),
    Color(0xFFCE93D8),
    Color(0xFFA5D6A7)
)

private fun getTopCardCategoryColor(category: String): Color {
    return categoryColorMap[category] ?: fallbackColors[
        Math.abs(category.hashCode()) % fallbackColors.size
    ]
}

private fun formatRecordDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun RankBadge(rank: Int) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        when (rank) {
            1 -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            2, 3 -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            else -> {
                Text(
                    text = rank.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CategoryIconCircle(category: String) {
    val bgColor = getTopCardCategoryColor(category)
    val label = category.firstOrNull()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun TopExpensesCard(
    expenses: List<ExpenseRecord>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本月支出排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            expenses.forEachIndexed { index, expense ->
                val rank = index + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RankBadge(rank = rank)
                    Spacer(modifier = Modifier.width(10.dp))
                    CategoryIconCircle(category = expense.category)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.merchant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = expense.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "¥${"%.2f".format(expense.amount)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = formatRecordDate(expense.recordedAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "查看全部记录 →",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}