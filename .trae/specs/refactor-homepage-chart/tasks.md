# Tasks

- [x] Task 1: 重构 HomeScreen 概览卡片为垂直弹性布局
  - [x] SubTask 1.1: 将 Row(weight=1f) 水平布局改为 Column(spacedBy=16dp) 垂直布局
  - [x] SubTask 1.2: 今日支出：titleMedium + displayLarge/Bold/error，内容居中
  - [x] SubTask 1.3: 本月支出：labelSmall + titleLarge/SemiBold/onSurface，内容居中
  - [x] SubTask 1.4: 移除不再需要的 Row 相关 import（如果无其他使用处）

- [x] Task 2: 新建 TrendChart 通用图表组件（TrendChart.kt）
  - [x] SubTask 2.1: 定义 ChartType enum { LINE, BAR }
  - [x] SubTask 2.2: 实现折线图组件，使用 MPAndroidChart LineChart + 数据标签
  - [x] SubTask 2.3: 实现柱状图组件，使用 MPAndroidChart BarChart + 数据标签
  - [x] SubTask 2.4: 使用 Crossfade(chartType) 包裹 LineChartView / BarChartView 切换
  - [x] SubTask 2.5: 为两种图表均设置 setTouchEnabled(false) 防止滑动冲突
  - [x] SubTask 2.6: 数据标签使用 IValueFormatter，格式化金额（¥XX.XX），字号 10sp

- [x] Task 3: 集成 TrendChart 到 HomeScreen
  - [x] SubTask 3.1: 添加 chartType 状态：var chartType by remember { mutableStateOf(ChartType.LINE) }
  - [x] SubTask 3.2: GlassCardSection 标题行右侧添加 IconButton 图标切换按钮组
  - [x] SubTask 3.3: 替换 MiniLineChart 调用为 TrendChart(data = trendData, chartType = chartType, modifier)

- [x] Fix: TrendChart valueFormatter 类型不兼容（匿名对象内联到 apply 块）
- [x] Fix: HomeScreen R.drawable 缺失 import

# Task Dependencies
- Task 3 depends on Task 2（需要 TrendChart 组件就绪才能引入）
- Task 1 独立，可与 Task 2 并行执行