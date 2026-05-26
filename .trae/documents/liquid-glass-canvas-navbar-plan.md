# 液态玻璃导航栏 Canvas 绘制方案 — 实施计划

## 目标
完全重写 `GlassNavigationBar.kt`，用 Canvas 五层绘制模拟液态玻璃物理质感，**不依赖 `Modifier.blur()`**。

---

## Step 1: 重写 GlassNavigationBar.kt — Canvas 五层液态玻璃绘制

### 1.1 导入新增
在现有导入基础上，新增以下 import：
```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeDraw
```

### 1.2 组件签名保持不变
```kotlin
@Composable
fun GlassNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

### 1.3 移除旧玻璃实现，替换为 Canvas 五层绘制

**旧代码删除**：第 58-93 行（整个 Box(matchParentSize) + shadow + clip + background + border 结构）

**新代码结构**：
```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
) {
    // === Canvas 五层液态玻璃 ===
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

        // 第1层：玻璃基底
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

        // 第2层：顶部高光带
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

        // 第3层：底部反光
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

        // 第4层：边缘描边（菲涅尔效应）
        drawRoundRect(
            color = Color.White.copy(alpha = if (isDark) 0.15f else 0.70f),
            cornerRadius = cr,
            style = Stroke(width = 1.dp.toPx())
        )

        // 第5层：顶部亮线（最亮的玻璃边缘反光）
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

    // === 导航项内容层 ===
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
```

### 1.4 GlassNavItem 子组件 — 添加选中态胶囊背景

**旧代码修改**：在 `GlassNavItem` 的 `Column` 外层包裹一个居中 Box，选中时显示胶囊背景：

```kotlin
@Composable
fun RowScope.GlassNavItem(...) {
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
                    indication = ripple(bounded = true, radius = 24.dp),
                    onClick = onClick
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(...)
            Spacer(modifier = Modifier.height(3.dp))
            Text(...)
        }
    }
}
```

**调整**：`GlassNavItem` 不再需要 `fillMaxHeight()` 和 `weight(1f)`（这些移到外层 Box），改为在 `Column` 上使用 `fillMaxWidth()`。

### 1.5 移除不再需要的 import
- 删除 `import androidx.compose.foundation.border`（不再使用 border Modifier）
- 删除 `import androidx.compose.foundation.background` 中不再需要的引用（需确认：`background` 在 `GlassNavItem` 中的选中态胶囊仍使用，所以保留）
- `animateColorAsState`, `tween`, `MaterialTheme`, `rememberRipple` 等保留

### 1.6 navShape 定义调整
移除旧的 `val navShape = RoundedCornerShape(32.dp)`（旧代码中用于 shadow/clip/border），新代码中 shadow 的 shape 仍需要它，所以**保留**。

---

## Step 2: MainActivity.kt 检查 — 确认无需修改

当前 MainActivity 中：
- `GlassNavigationBar` 通过 `modifier = Modifier.align(Alignment.BottomCenter)` 悬浮定位 ✅
- `contentWindowInsets = WindowInsets(0, 0, 0, 0)` ✅
- `bottomBar = {}` ✅
- `Crossfade(targetState = selectedScreen)` 未改动 ✅

**结论**：MainActivity.kt 不需要任何修改，GlassNavigationBar 接口签名保持不变。

---

## 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/GlassNavigationBar.kt` | 重写 | Canvas 五层绘制 + 选中胶囊背景 |
| `MainActivity.kt` | 不变 | 接口兼容，无需修改 |

---

## 关键设计决策

1. **五层 Canvas 绘制顺序**：基底 → 高光 → 底部反光 → 边缘描边 → 顶部亮线（后绘制的覆盖在前之上）
2. **深浅色适配**：`isDark` 布尔值控制各层的 alpha 值，深色模式下透明度更低以保持对比度
3. **不依赖 blur**：所有玻璃质感完全通过渐变色 + 描边 + 阴影模拟
4. **shadow 放在 Canvas modifier 上**：确保整个玻璃容器有一个柔和的悬浮投影
5. **navShape 保留**：`RoundedCornerShape(32.dp)` 用于 shadow shape 参数