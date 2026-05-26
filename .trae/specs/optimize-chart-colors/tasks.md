# Tasks

- [x] Task 1: 优化 TrendChart.kt 颜色方案
  - [x] 折线图 LINE 模式：将 `color = primaryColorArgb` 改为 `color = tealColorArgb`（0xFF26A69A）
  - [x] 折线图 fillColor 改为 `tealColor.copy(alpha = 0.2f).toArgb()`
  - [x] 折线图 lineWidth 从 2f 改为 2.5f
  - [x] 折线图 setDrawCircles 从 false 改为 true，circleRadius 设为 4f，circleColor 设为 teal
  - [x] 折线图添加 `setCircleColor(tealColorArgb)` 和 `circleHoleColor = tealColorArgb`
  - [x] 柱状图 BAR 模式：将 `color = primaryColorArgb` 改为多色方案
  - [x] BAR 模式从 `dataSet.color = primaryColorArgb` 改为 `dataSet.colors = barColors.map { it.toArgb() }`
  - [x] 定义暖色调色板 `barColors`（7色）
  - [x] 确认折线图颜色在深色模式下无需特殊处理（青绿色在深色背景上同样可读）

- [x] Task 2: 优化 MonthlyBarChart.kt 多色方案
  - [x] 移除 `val primaryColor` 和 `val dimColor` 变量
  - [x] 新增暖色调色板 `monthlyBarColors`（7色）
  - [x] 将 `colors = entries.mapIndexed { ... primary/dim }` 替换为 `colors = monthlyBarColors.map { it.toArgb() }`
  - [x] 确认 RoundedBarChartRenderer 中 `isSingleColor` 逻辑自动适配多色

# Task Dependencies
- Task 1 和 Task 2 可并行执行（不同文件）