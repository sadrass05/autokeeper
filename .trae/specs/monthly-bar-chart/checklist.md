# Checklist

- [x] MonthlyExpense 数据类定义在 MainViewModel.kt 中，包含 year、month、amount、label 四个字段
- [x] _monthlyStats MutableStateFlow 和 monthlyStats StateFlow 已在 MainViewModel 中声明
- [x] loadMonthlyStats() 方法在 init{} 和 loadData() 中都有调用
- [x] 月度标签生成逻辑正确：当月/1月显示 "年\n月"，其余显示 "X月"
- [x] loadMonthlyStats() 遍历最近6个月且顺序正确（最早的在前，当前月在最后）
- [x] MonthlyBarChart.kt 文件创建于 ui/components/ 目录下
- [x] RoundedBarChartRenderer 继承 BarChartRenderer 并实现柱子圆角绘制
- [x] 当前月柱子颜色为 MaterialTheme.colorScheme.primary
- [x] 历史月柱子颜色为 primary.copy(alpha = 0.35f)
- [x] 柱子顶部金额标签显示 "¥XXX" 格式，字体 10sp
- [x] 自定义 MarkerView 实现点击 tooltip "X月共支出 ¥XXX.XX"
- [x] 图表背景透明，无描述文字，无图例
- [x] Y轴隐藏（无标签），X轴仅显示月份标签
- [x] 图表有 animateY(500) 入场动画
- [x] HomeScreen 中月度柱状图卡片位于"支出趋势"图表之后、"当日支出分类"之前
- [x] 卡片使用 Card 容器，标题为"月度支出对比"
- [x] monthlyStats 数据通过 collectAsStateWithLifecycle() 从 ViewModel 获取
- [x] 无编译错误，无未使用导入