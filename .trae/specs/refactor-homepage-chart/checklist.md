# Checklist

- [x] HomeScreen 概览区域使用 Column(spacedBy=16.dp) 垂直布局
- [x] 今日支出卡片在上，本月支出在下，均 fillMaxWidth
- [x] 今日支出金额使用 displayLarge + Bold + error 色
- [x] 本月支出金额使用 titleLarge + SemiBold + onSurface 色
- [x] 本月支出标签使用 labelSmall
- [x] TrendChart.kt 新建完成，含 ChartType enum
- [x] TrendChart 使用 Crossfade(chartType) 切换折线/柱状
- [x] 折线图使用 MPAndroidChart LineChart，CUBIC_BEZIER 模式，渐变填充
- [x] 柱状图使用 MPAndroidChart BarChart
- [x] 两种图表均 setDrawValues(true)，数据标签显示 ¥XX.XX
- [x] 两种图表均 setTouchEnabled(false) 防止滑动冲突
- [x] 图标按钮组在卡片标题行右侧，选中态 primary 高亮
- [x] HomeScreen 持有 chartType 状态，默认 ChartType.LINE
- [x] MiniLineChart 调用已替换为 TrendChart
- [x] 构建通过无编译错误