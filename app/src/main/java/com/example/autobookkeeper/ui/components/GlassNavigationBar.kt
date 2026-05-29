package com.example.autobookkeeper.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(
    val label: String,
    val icon: Int,
    val selectedIcon: Int
)

@Composable
fun GlassNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val navShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 20.dp,
                    shape = navShape,
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.4f)
                                else Color.Black.copy(alpha = 0.12f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.2f)
                                   else Color.Black.copy(alpha = 0.06f)
                )
        ) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            val cr = CornerRadius(r, r)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color(0xFF2A2A2A).copy(alpha = 0.90f),
                        Color(0xFF1A1A1A).copy(alpha = 0.95f)
                    ) else listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.82f),
                        Color(0xFFF0F0F0).copy(alpha = 0.88f)
                    )
                ),
                cornerRadius = cr
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.22f else 0.65f),
                        Color.White.copy(alpha = 0f)
                    ),
                    startY = 0f,
                    endY = h * 0.55f
                ),
                cornerRadius = cr
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = if (isDark) 0.06f else 0.20f)
                    ),
                    startY = h * 0.7f,
                    endY = h
                ),
                cornerRadius = cr
            )

            drawRoundRect(
                color = Color.White.copy(alpha = if (isDark) 0.15f else 0.70f),
                cornerRadius = cr,
                style = Stroke(width = 1.dp.toPx())
            )

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.90f),
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.90f),
                        Color.White.copy(alpha = 0f)
                    ),
                    startX = r,
                    endX = w - r
                ),
                start = Offset(r, 1.dp.toPx()),
                end = Offset(w - r, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                GlassNavItem(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
fun RowScope.GlassNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        animationSpec = tween(300),
        label = "iconColor"
    )

    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(selectedBg)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (isSelected) item.selectedIcon else item.icon
                ),
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}