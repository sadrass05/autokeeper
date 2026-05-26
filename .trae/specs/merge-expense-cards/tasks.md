# Tasks

- [x] Task 1: 合并支出概览卡片
  - [x] 删除 HomeScreen.kt 中第 100-154 行的两个独立 GlassCard 及其外层 Column
  - [x] 替换为单个 Card 组件，内部垂直排列今日支出 + 分割线 + 本月支出
  - [x] 使用 `CardDefaults.cardElevation(defaultElevation = 2.dp)` 替代 GlassCard 的高斯模糊效果
  - [x] 使用 `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)`
  - [x] Card shape 使用 `RoundedCornerShape(20.dp)`
  - [x] Card 内 `padding(vertical = 24.dp, horizontal = 20.dp)`，`horizontalAlignment = Alignment.CenterHorizontally`
  - [x] 今日支出：`bodyMedium` 标签 + `displaySmall` Bold 金额（error 色）
  - [x] 分割线：`HorizontalDivider(fillMaxWidth(0.6f), thickness=0.5dp, outlineVariant)`
  - [x] 本月支出：Row 中 `bodySmall` 标签 + `titleMedium` SemiBold 金额（onSurface 色）
  - [x] 确认 `dailyExpense` 和 `monthlyExpense` 数据绑定正确
  - [x] 确认不需要新增/删除导入（Card/CardDefaults/HorizontalDivider 已存在）

# Task Dependencies
- 无依赖，单文件修改