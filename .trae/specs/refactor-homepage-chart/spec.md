# 首页布局重构与图表组件优化 Spec

## Why
首页概览卡片为水平等宽布局，不能区分"今日支出"（核心指标）与"本月支出"（辅助指标）的视觉层级。支出趋势图表缺少数据标签和图表类型切换能力。

## What Changes
- 概览卡片从 Row 等宽改为 Column 垂直弹性布局，今日支出占更大视觉比重
- 今日支出使用 displayLarge 大号字体，本月支出缩小为 titleLarge
- 新建 TrendChart 通用图表组件，支持折线图/柱状图动态切换
- 图表增加数据标签（始终悬浮显示），保留 MPAndroidChart 现有的滑动冲突防护
- 使用 Crossfade 实现图表类型平滑过渡动画
- 图表标题行右侧增加图标按钮组（折线/柱状切换）
- 保留现有 MiniLineChart.kt 作为备用，新组件写入 TrendChart.kt

## Impact
- Affected specs: ui-overhaul, refactor-transaction-item, material3-theme-system
- Affected code: HomeScreen.kt（概览卡片 + 图表区域），MiniLineChart.kt（重构）
- New files: ui/components/TrendChart.kt

---

## ADDED Requirements

### Requirement: 概览卡片垂直布局
系统 SHALL 将"今日支出"与"本月支出"卡片改为 Column 垂直分布，今日支出卡片占据更大的视觉比重。

#### Scenario: 垂直布局
- **WHEN** 渲染首页概览区域
- **THEN** 使用 Column(verticalArrangement = spacedBy(16.dp)) 包裹两张 GlassCard
- **AND** 今日支出在上，本月支出在下
- **AND** 卡片均 fillMaxWidth，不设 weight(1f) 等宽

#### Scenario: 今日支出视觉重心
- **WHEN** 渲染今日支出卡片
- **THEN** 标签使用 titleMedium + onSurfaceVariant
- **AND** 金额使用 displayLarge + Bold + error 色
- **AND** 内容水平居中

#### Scenario: 本月支出缩小
- **WHEN** 渲染本月支出卡片
- **THEN** 标签使用 labelSmall + onSurfaceVariant
- **AND** 金额使用 titleLarge + SemiBold + onSurface
- **AND** 内容水平居中

### Requirement: 图表数据标签
系统 SHALL 在折线图和柱状图上为每个数据点显示金额标签。

#### Scenario: 标签始终显示
- **WHEN** 渲染趋势图表
- **THEN** 每个数据点上方显示具体金额数值
- **AND** 标签样式为小字号（10sp）半透明
- **AND** 使用 IValueFormatter 格式化标签

### Requirement: 图表类型切换
系统 SHALL 提供图标按钮组在折线图和柱状图之间切换。

#### Scenario: 切换控件
- **WHEN** 渲染图表区域标题行
- **THEN** 标题右侧显示两个 IconButton（line_chart / bar_chart 图标）
- **AND** 当前选中类型使用 primary 色高亮
- **AND** 点击切换时使用 Crossfade 平滑过渡动画

#### Scenario: 默认图表类型
- **WHEN** 首页首次加载
- **THEN** 默认显示折线图

### Requirement: ChartType 枚举
系统 SHALL 使用 enum 管理图表类型状态。

#### Scenario: ChartType 定义
- **WHEN** 引用图表类型
- **THEN** ChartType.LINE 表示折线图
- **AND** ChartType.BAR 表示柱状图

### Requirement: TrendChart 通用组件
系统 SHALL 封装 TrendChart 组件，根据 ChartType 动态渲染不同图表。

#### Scenario: 组件签名
- **WHEN** 调用 TrendChart
- **THEN** 参数为 data: List<DailyExpense>, chartType: ChartType, modifier: Modifier
- **AND** 内部使用 Crossfade(chartType) 切换 LineChartView / BarChartView

### Requirement: 图表性能优化
系统 SHALL 使用 key() + Crossfade 避免 AndroidView 不必要的重组。

#### Scenario: 实例复用
- **WHEN** 切换图表类型
- **THEN** Crossfade 自动销毁旧实例、创建新实例
- **AND** 同类型数据更新时仅调 chart.invalidate()，不重建 AndroidView

### Requirement: 滑动冲突防护
系统 SHALL 保持图表内部触摸控制为禁用状态。

#### Scenario: 无冲突滚动
- **WHEN** 页面 LazyColumn 滚动
- **THEN** 图表作为静态内容随页面整体滚动
- **AND** setTouchEnabled(false) 阻止图表内部手势

## MODIFIED Requirements

### Requirement: HomeScreen 概览区域
系统 SHALL 将概览区域从 Row(weight=1f) 水平布局改为 Column 垂直弹性布局。

#### Scenario: 概览区域重构
- **WHEN** 首页渲染
- **THEN** 移除原 Row 中两个 weight(1f) GlassCard
- **AND** 改为 Column(spacedBy=16dp) 包裹两张 GlassCard(fillMaxWidth)

### Requirement: HomeScreen 图表区域
系统 SHALL 替换 MiniLineChart 为 TrendChart，并添加图表类型切换和状态管理。

#### Scenario: 图表区域
- **WHEN** 渲染支出趋势区域
- **THEN** GlassCardSection 标题行右侧增加图标按钮组
- **AND** 卡片内使用 TrendChart(data, chartType, modifier)
- **AND** HomeScreen 持有 `var chartType by remember { mutableStateOf(ChartType.LINE) }`

## REMOVED Requirements

### Requirement: MiniLineChart 直接使用
**Reason**: 功能被 TrendChart 取代。
**Migration**: HomeScreen 中 `MiniLineChart(values, dateLabels, modifier)` 改为 `TrendChart(trendData, chartType, modifier)`。保留 MiniLineChart.kt 文件不删除。