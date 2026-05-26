# UI 全面翻新设计文档

## 1. 液态毛玻璃胶囊导航栏

### 组件：GlassNavigationBar
独立于 Scaffold bottomBar 的浮动组件，使用 Box 叠加在页面内容上方底部。

```
┌──────────────────────────────────────┐
│         页面内容区域                    │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  首页    记录    理财    设置   │  │ ← 胶囊导航栏
│  └────────────────────────────────┘  │
│         ▲ navigationBarsPadding      │
└──────────────────────────────────────┘
```

### 视觉参数

| 属性 | 深色模式 | 浅色模式 |
|------|---------|---------|
| 背景模糊 | `graphicsLayer { renderEffect = BlurEffect(20f, 20f) }` (API 31+)，回退：半透明 | 同左 |
| 基底色 | DarkSurface `#161A22`.copy(alpha=0.85f) | White.copy(alpha=0.75f) |
| 描边 | White.copy(alpha=0.08f), 1dp | White.copy(alpha=0.4f), 1dp |
| 圆角 | RoundedCornerShape(24.dp) | 同左 |
| 阴影 | shadowElevation=8dp, ambientColor=Black.copy(0.15f) | shadowElevation=4dp |
| Padding | horizontal=6dp, vertical=4dp | 同左 |
| 左右缩进 | 16dp | 同左 |

### 动画参数
- **弹性缩放**：`Animatable(1f)` + `spring(dampingRatio=0.5f, stiffness=300f)`
- **颜色渐变**：`animateColorAsState(300ms, tween())`
- **选中指示器**：底部 3dp 高度琥珀金圆角条，`AnimatedVisibility` + `fadeIn/fadeOut`

### 导航项结构
```kotlin
NavigationBarItem(
    icon = { Image(icon, modifier = Modifier.scale(scaleAnim)) },
    label = { Text(label, color = labelColor) },
    colors = colors,
    indicatorContent = { IndicatorBar() }
)
```

---

## 2. 调色板升级

### 新增常量（Color.kt）

| 常量 | 值 | 用途 |
|------|-----|------|
| DeepBlue | `#2563EB` | secondary（深色） |
| DeepBlueLight | `#3B82F6` | secondary（浅色） |
| DeepBlueContainer | `#0F1E40` | secondaryContainer 深色 |
| DeepBlueLightContainer | `#DBEAFE` | secondaryContainer 浅色 |
| OnDeepBlue | `#FFFFFF` | onSecondary |
| OnDeepBlueContainer | `#1E40AF` | onSecondaryContainer |
| WarmBackground | `#F5F5F7` | 浅色背景（更暖） |
| DarkBackgroundDeep | `#0B0E14` | 深色背景（更深） |
| DarkSurfaceDeep | `#161A22` | 深色表面（蓝灰） |
| DarkSurfaceVariantDeep | `#1E2430` | 深色表面变体 |
| LightSurfaceAlt | `#F0F0F3` | 浅色表面变体（更暖） |

### ColorScheme 映射

```
DarkColorScheme:
  primary = AmberGold, secondary = DeepBlue
  background = DarkBackgroundDeep, surface = DarkSurfaceDeep
  surfaceVariant = DarkSurfaceVariantDeep

LightColorScheme:
  primary = AmberGold, secondary = DeepBlueLight
  background = WarmBackground, surface = White
  surfaceVariant = LightSurfaceAlt
```

---

## 3. 通用卡片组件

### GlassCard
```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassCardSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, ...)
        Spacer(Modifier.height(8.dp))
        GlassCard { content() }
    }
}
```

---

## 4. 字体层级优化

| 排版槽位 | 修改 | 字重 | 字号 |
|---------|------|------|------|
| headlineLarge | SemiBold → Bold | Bold | 28sp |
| titleLarge | Medium → SemiBold | SemiBold | 18sp |
| headineMedium | 保持 | SemiBold | 22sp |
| titleMedium | 保持 | Medium | 15sp |
| bodyLarge | 保持 | Normal | 15sp |
| bodyMedium | letterSpacing=0.25sp | Normal | 13sp |

---

## 5. 各页面改造要点

### 首页（HomeScreen）
- 今日支出/本月支出 → GlassCard 包裹（两个 Column 并排）
- `SectionTitle` + MiniLineChart → GlassCardSection 包裹
- `SectionTitle` + CategoryDonutChart → GlassCardSection 包裹
- TransactionItem → 每项 .clip(RoundedCornerShape(12.dp)) + .background(surface)，项间 6dp 间距
- 底部按钮保留 OutlinedButton 样式，增加圆角 12dp

### 记录页（RecordsScreen）
- 筛选栏 → GlassCard 包裹（padding 12dp）
- SwipeableRecordItem → SwipeToDismissBox 外层不透明 background(surface) + clip(12dp)
- 列表项间 padding 4dp

### 理财页（FinanceScreen）
- 总市值概览 4 列 → 保持现有 RoundedCornerShape(16dp) + surface 背景
- 净收益公式条 → 统一 16dp 圆角
- 持仓列表项 → 统一 12dp 圆角 + 项间 6dp 间距

### 设置页（SettingsScreen）
- 每个 Section（通知/数据/外观/关于）→ GlassCard 包裹整个设置组
- SectionHeader 在 Card 上方
- SettingsRow 保持现有 padding 不变

---

## 6. 全局间距标准

| 层级 | 值 |
|------|-----|
| 页面水平 padding | 20dp |
| 卡片间距（section 间） | 12dp |
| 卡片内 padding | 16dp |
| 列表项内 padding | 14dp |
| 列表项间距 | 6dp |
| 按钮圆角 | 12dp |
| 卡片圆角 | 16dp |
| 导航栏圆角 | 24dp |
