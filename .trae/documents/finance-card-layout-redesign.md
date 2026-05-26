# FinanceScreen 资产概览卡片排版重新设计

## 目标
解决 FinanceScreen 顶部四列资产数据因数字过大（¥6773.14）导致的文字溢出换行问题。

## 当前状态
- [FinanceScreen.kt:157-187](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt#L157-L187)：4 个 `StatColumn(weight=1f)` 挤在一个 `Row` 中
- `titleLarge`（~22sp）+ 4 列均分 → `¥6,773.14`（12 字符）强制换行
- `StatColumn`（L579-603）标签 `bodySmall` + 数值 `titleLarge Bold`

## 决策
| 决策 | 选择 |
|------|------|
| 布局方案 | **方案D：混合优化** — 2×2 网格 + 竖向分隔线 + 净收益高亮底部条 |
| 数值策略 | **两者结合** — ≥10000 格式化为万/亿 + 字号从 titleLarge 降到 titleMedium |

---

## 变更范围

### 仅修改 1 个文件
`app/src/main/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt`

### 不改的文件
- `GlassCard.kt` — 卡片容器不变
- `Theme.kt` / `Color.kt` — 颜色常量不变，复用现有 `tertiary`(ProfitGreen) / `error`(ErrorRed)

---

## 具体变更

### 1. 新增 `formatFinanceAmount(value: Double): String` 函数

```kotlin
private fun formatFinanceAmount(value: Double): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs >= 100_000_000 -> "${sign}¥${"%.2f".format(abs / 100_000_000)}亿"
        abs >= 10_000 -> "${sign}¥${"%.2f".format(abs / 10_000)}万"
        else -> "${sign}¥${"%.2f".format(abs)}"
    }
}
```

输入/输出示例：

| 输入 | 输出 |
|------|------|
| 6773.14 | `¥6773.14` |
| 12345.67 | `¥1.23万` |
| -500.00 | `-¥500.00` |
| 150000000 | `¥1.50亿` |
| 0.00 | `¥0.00` |

### 2. 替换资产概览卡片布局（L157-187）

**替换前**（1行4列）：
```kotlin
GlassCard(contentPadding = PaddingValues(16.dp)) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
        StatColumn(label="总市值", value="¥${"%.2f".format(totalCurrentValue)}", modifier=Modifier.weight(1f))
        StatColumn(label="累计收益", ..., modifier=Modifier.weight(1f))
        StatColumn(label="理财收益支出", ..., modifier=Modifier.weight(1f))
        StatColumn(label="剩余净收益", ..., modifier=Modifier.weight(1f))
    }
}
```

**替换后**（2×2 网格 + 竖向分隔线 + 净收益高亮条）：

```kotlin
GlassCard(contentPadding = PaddingValues(0.dp)) {
    Column {
        // 第1行：总市值 | 累计收益
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                label = "总市值",
                value = formatFinanceAmount(totalCurrentValue),
                modifier = Modifier.weight(1f),
                alignment = Alignment.CenterHorizontally
            )
            // 竖向分隔线
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            StatCell(
                label = "累计收益",
                value = formatFinanceAmount(totalProfit),
                valueColor = if (totalProfit > 0) MaterialTheme.colorScheme.tertiary
                             else if (totalProfit < 0) MaterialTheme.colorScheme.error
                             else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                alignment = Alignment.CenterHorizontally
            )
        }

        // 横向分隔线
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
        )

        // 第2行：理财收益支出 | 剩余净收益
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                label = "理财收益支出",
                value = formatFinanceAmount(financeExpense),
                valueColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                alignment = Alignment.CenterHorizontally
            )
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            StatCell(
                label = "剩余净收益",
                value = formatFinanceAmount(netFinanceProfit),
                valueColor = if (netFinanceProfit > 0) MaterialTheme.colorScheme.tertiary
                             else if (netFinanceProfit < 0) MaterialTheme.colorScheme.error
                             else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                alignment = Alignment.CenterHorizontally
            )
        }

        // 底部净收益高亮条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (netFinanceProfit >= 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "净收益 ${formatFinanceAmount(netFinanceProfit)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (netFinanceProfit > 0) MaterialTheme.colorScheme.tertiary
                        else if (netFinanceProfit < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 3. 新增 `StatCell` 组件（替换旧 `StatColumn`）

```kotlin
@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
```

**关键差异 vs 旧 `StatColumn`：**
- 字号：`titleLarge` → `titleMedium`（~22sp → ~16sp）
- `maxLines = 1` + `softWrap = false` 防止溢出换行
- 标签字号：`bodySmall` → `labelSmall`（更小，节省垂直空间）
- 标签数值间距：4dp → 2dp

### 4. 删除旧代码

- 删除旧的 `StatColumn` Composable（L579-603），被新的 `StatCell` 替代
- 删除紧接在资产卡片后面的"净收益 = 理财收益 - 理财收益支出 =" 提示卡片（L192-208），因为新设计的底部高亮条已经包含净收益信息

### 5. 新增 import

```kotlin
import androidx.compose.foundation.background
```

已有 `HorizontalDivider` 的 import（L45），无需新增。

---

## 布局结构图示

```
┌─────────────────────────────────────┐
│  GlassCard (padding = 0dp)         │
│  ┌───────────┬─┬───────────────┐   │
│  │  总市值    │ │  累计收益     │   │  ← 第1行 + 竖线分隔
│  │  ¥6773.14 │ │  ¥1.23万     │   │
│  ├───────────┴─┴───────────────┤   │
│  │──── horizontal divider ─────│   │
│  ├───────────┬─┬───────────────┤   │
│  │ 理财收益支出│ │ 剩余净收益   │   │  ← 第2行 + 竖线分隔
│  │  -¥500.00 │ │  ¥6273.14    │   │
│  ├───────────┴─┴───────────────┤   │
│  │  ▓▓ 净收益 ¥6273.14 ▓▓     │   │  ← 底部高亮条
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 验证步骤
1. 视觉验证：小数字（<1000）不溢出且排版美观
2. 视觉验证：中等数字（¥6773.14）在 2×2 网格中不溢出
3. 视觉验证：大数字（¥12345.67 → ¥1.23万）正确格式化
4. 视觉验证：负数用红色，正数用绿色，零用默认色
5. 代码验证：无未使用 import，无编译错误
6. 功能验证：净收益卡片移除后，公式信息仍在底部高亮条中呈现
