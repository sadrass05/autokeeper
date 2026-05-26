package com.example.autobookkeeper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryRing(
    expense: Double,
    profit: Double,
    modifier: Modifier = Modifier
) {
    val netAmount = profit - expense
    val total = (expense + profit).coerceAtLeast(1.0)
    val expenseRatio = (expense / total).toFloat().coerceIn(0.05f, 0.95f)
    val profitRatio = (profit / total).toFloat().coerceIn(0.05f, 0.95f)

    val expenseSweep = remember { Animatable(0f) }
    val profitSweep = remember { Animatable(0f) }

    LaunchedEffect(expenseRatio, profitRatio) {
        expenseSweep.animateTo(expenseRatio * 360f, tween(800))
        profitSweep.animateTo(profitRatio * 360f, tween(800))
    }

    val netText = "¥${"%.0f".format(netAmount)}"
    val labelText = if (netAmount >= 0) "净盈余" else "净支出"
    val labelColor = if (netAmount >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(180.dp)) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 16.dp.toPx()
            val gap = 8f
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            drawArc(
                color = errorColor,
                startAngle = -90f + gap,
                sweepAngle = expenseSweep.value - gap * 2,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color =tertiaryColor,
                startAngle = -90f + expenseSweep.value + gap,
                sweepAngle = profitSweep.value - gap * 2,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = netText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = labelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
        }
    }
}