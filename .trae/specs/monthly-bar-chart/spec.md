# 月度支出对比柱状图 Spec

## Why
HomeScreen 当前只有"支出趋势（最近7天）"的折线/柱状图，缺少月度维度的支出对比视图。用户需要一眼看到最近6个月支出变化趋势，并区分当前月和历史月。

## What Changes
- 新增 `MonthlyExpense` 数据类（ViewModel 层）
- 在 `MainViewModel` 中新增 `monthlyStats` StateFlow + `loadMonthlyStats()` 方法
- 新增 `MonthlyBarChart.kt` 组件文件，使用 MPAndroidChart BarChart
- 在 `HomeScreen.kt` 中将月度柱状图卡片插入"支出趋势"图表下方
- DAO/Repository 已有 `getTotalExpenseByMonth(startTime, endTime)` 方法，无需新增

## Impact
- Affected specs: 无（新功能，与现有 spec 无交叉）
- Affected code:
  - **新增**: `ui/components/MonthlyBarChart.kt`
  - **修改**: `ui/viewmodel/MainViewModel.kt`（新增 data class + StateFlow + 方法）
  - **修改**: `ui/screen/HomeScreen.kt`（插入新图表卡片）

## ADDED Requirements

### Requirement: MonthlyExpense Data Class
系统 SHALL 在 MainViewModel.kt 中定义 `MonthlyExpense` 数据类，包含 `year`、`month`、`amount`、`label` 字段。

#### Scenario: 数据类定义
- **GIVEN** 需要表示单月支出数据
- **WHEN** 定义 MonthlyExpense
- **THEN** 包含 `year: Int`、`month: Int`、`amount: Float`、`label: String`

### Requirement: Monthly Stats StateFlow
系统 SHALL 在 MainViewModel 中新增 `monthlyStats: StateFlow<List<MonthlyExpense>>`，暴露最近6个月的支出汇总数据。

#### Scenario: 加载月度统计数据
- **GIVEN** ViewModel 初始化
- **WHEN** `loadMonthlyStats()` 被调用
- **THEN** 查询最近6个月（当前月往前推5个月）的支出数据，按月分组求和
- **AND** `_monthlyStats.value` 被赋值为 `List<MonthlyExpense>`

### Requirement: 月度标签生成
系统 SHALL 为每月生成合适的显示标签：
- 当月（i==0）或月份为1月时，标签格式为 `"年\n月"`（如 "2026\n1月"）
- 其他月份标签格式为 `"X月"`（如 "5月"）
- 使用 `\n` 换行分隔年份和月份

#### Scenario: 跨年标签
- **GIVEN** 统计数据跨年份（如2025年12月和2026年1月）
- **WHEN** 生成标签
- **THEN** 1月标签显示为 "2026\n1月"，12月显示为 "12月"

### Requirement: 月度柱状图组件
系统 SHALL 提供 `MonthlyBarChart` Composable 组件，接收 `List<MonthlyExpense>` 数据并渲染 MPAndroidChart BarChart。

#### Scenario: 图表基本渲染
- **GIVEN** 传入6个月的数据
- **WHEN** 组件渲染
- **THEN** 显示6根柱子，X轴底部显示月份标签，Y轴隐藏，背景透明

### Requirement: 柱子颜色区分
系统 SHALL 对当前月和历史月使用不同颜色：
- 当前月（数据最后一项）：`MaterialTheme.colorScheme.primary`
- 历史月：`primary.copy(alpha = 0.35f)`

#### Scenario: 当前月高亮
- **GIVEN** monthlyStats 包含6个月数据
- **WHEN** 渲染柱状图
- **THEN** 最后一个月（当前月）柱子为 `primary` 色
- **AND** 前5个月柱子为 `primary.copy(alpha = 0.35f)`

### Requirement: 柱子圆角
系统 SHALL 通过自定义 `RoundedBarChartRenderer` 实现柱子顶部圆角效果（4dp 圆角半径）。

#### Scenario: 圆角渲染
- **GIVEN** BarChart 已设置自定义 renderer
- **WHEN** 柱子被绘制
- **THEN** 柱子顶部呈现圆角效果

### Requirement: 柱子顶部金额标签
系统 SHALL 在每根柱子顶部显示该月金额，格式为 "¥XXX"，字体大小 10sp，颜色与柱子颜色一致。

#### Scenario: 金额标签显示
- **GIVEN** 某月支出为 ¥1234.56
- **WHEN** 渲染柱子
- **THEN** 柱子顶部显示 "¥1234.56"，字体 10sp

### Requirement: 柱状图卡片容器
系统 SHALL 使用 `Card` 容器包裹图表，卡片包含标题"月度支出对比"和图表内容。

#### Scenario: 卡片展示
- **GIVEN** 月度柱状图渲染
- **WHEN** 用户查看 HomeScreen
- **THEN** 看到标题"月度支出对比"的卡片，内含 BarChart

### Requirement: 点击 Tooltip
系统 SHALL 在用户点击柱子时，通过自定义 `MarkerView` 显示 tooltip 气泡，内容为 "X月共支出 ¥XXX.XX"。

#### Scenario: 点击显示详情
- **GIVEN** 用户点击某根柱子
- **WHEN** onChartValueSelected 回调触发
- **THEN** 柱子顶部弹出 tooltip 显示 "X月共支出 ¥XXX.XX"

### Requirement: HomeScreen 集成
系统 SHALL 在 HomeScreen LazyColumn 中插入月度柱状图卡片，位置在"支出趋势"图表之后、"当日支出分类"之前。

#### Scenario: 页面布局
- **GIVEN** HomeScreen 渲染
- **WHEN** 用户滚动页面
- **THEN** 依次看到：支出数据卡片 → 支出趋势图表 → 月度支出对比 → 当日支出分类 → 最近交易