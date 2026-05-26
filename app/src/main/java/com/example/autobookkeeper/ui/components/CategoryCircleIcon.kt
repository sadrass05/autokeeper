package com.example.autobookkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getCategoryColor(category: String): Color = when (category) {
    "餐饮" -> Color(0xFF4CAF50)
    "购物" -> Color(0xFF2196F3)
    "交通" -> Color(0xFFFF9800)
    "娱乐" -> Color(0xFF9C27B0)
    "教育" -> Color(0xFF00BCD4)
    "医疗" -> Color(0xFFF44336)
    "理财" -> Color(0xFF607D8B)
    "日用" -> Color(0xFF795548)
    else -> Color(0xFF9E9E9E)
}

@Composable
fun CategoryCircleIcon(
    category: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = getCategoryColor(category)
    val label = category.firstOrNull()?.toString() ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}