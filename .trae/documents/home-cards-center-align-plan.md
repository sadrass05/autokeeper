# HomeScreen 支出卡片居中对齐优化

## 当前状态

HomeScreen.kt L102-138，两个 `GlassCard` 内的 `Column` 已有 `horizontalAlignment = Alignment.CenterHorizontally`，但：
- `Text` composable 缺少 `textAlign = TextAlign.Center`
- `Column` 缺少 `Modifier.fillMaxWidth()`，导致包裹内容宽度时居中效果不明显
- 字体层级未形成视觉主次

## 修改内容

**仅涉及文件**: `app/src/main/java/com/example/autobookkeeper/ui/screen/HomeScreen.kt`

### 步骤 1：修改"今日支出"卡片（L102-119）

改前：
```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(12.dp)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "今日支出",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¥${"%.2f".format(dailyExpense)}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}
```

改为：
```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "今日支出",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${"%.2f".format(dailyExpense)}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
```

关键变更：
- `titleMedium` → `bodyMedium`（标签更轻量）
- `displayLarge` → `displaySmall`（金额稍缩小但仍突出）
- 新增 `Modifier.fillMaxWidth()` 到 Column
- 新增 `textAlign = TextAlign.Center` 到两个 Text
- `contentPadding` 从 `12.dp` → `horizontal=20.dp, vertical=16.dp`
- `Spacer(8.dp)` → `Spacer(4.dp)`（紧凑间距）

### 步骤 2：修改"本月支出"卡片（L121-138）

改前：
```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(12.dp)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "本月支出",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "¥${"%.2f".format(monthlyExpense)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

改为：
```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "本月支出",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "¥${"%.2f".format(monthlyExpense)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
```

关键变更：
- `labelSmall` → `bodySmall`（标签稍大）
- `titleLarge` → `headlineMedium`（金额稍缩小，形成主次）
- 新增 `Modifier.fillMaxWidth()` 到 Column
- 新增 `textAlign = TextAlign.Center` 到两个 Text
- 新增 `Spacer(height=2.dp)` 在标签和金额之间
- `contentPadding` 从 `12.dp` → `horizontal=20.dp, vertical=14.dp`

### 步骤 3：新增导入

需要确保以下导入存在于 HomeScreen.kt：
- `androidx.compose.ui.text.style.TextAlign` — `TextAlign.Center` 所需

检查现有导入：当前文件未导入 `TextAlign`，需新增：
```kotlin
import androidx.compose.ui.text.style.TextAlign
```

## 视觉层级设计

| 卡片 | 标签字体 | 金额字体 | 视觉权重 |
|------|----------|----------|----------|
| 今日支出 | `bodyMedium` | `displaySmall` | **主导** — 金额最大、红色突出 |
| 本月支出 | `bodySmall` | `headlineMedium` | 次要 — 金额中等、常规色 |

## 预期效果

1. 两个卡片的标签和金额数字水平居中显示
2. 今日支出金额以 `displaySmall + 红色` 作为视觉焦点
3. 本月支出金额以 `headlineMedium + 常规色` 作为次要信息
4. 清晰的视觉主次层级