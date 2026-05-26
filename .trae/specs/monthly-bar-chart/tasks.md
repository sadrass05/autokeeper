# Tasks

- [x] Task 1: 在 MainViewModel 中新增 MonthlyExpense 数据类和月度统计功能
  - [x] 在 MainViewModel.kt 中新增 `data class MonthlyExpense(val year: Int, val month: Int, val amount: Float, val label: String)`
  - [x] 新增 `private val _monthlyStats = MutableStateFlow<List<MonthlyExpense>>(emptyList())`
  - [x] 新增 `val monthlyStats: StateFlow<List<MonthlyExpense>> = _monthlyStats.asStateFlow()`
  - [x] 新增 `private fun loadMonthlyStats()` 方法：遍历最近6个月，调用 `expenseRepository.getTotalExpenseByMonth(start, end)` 获取每月总额，生成 label（当月或1月用 "年\n月" 格式，其余用 "X月"），存入 `_monthlyStats`
  - [x] 在 `init{}` 和 `loadData()` 中调用 `loadMonthlyStats()`

- [x] Task 2: 创建 MonthlyBarChart.kt 图表组件
  - [x] 新建文件 `app/src/main/java/com/example/autobookkeeper/ui/components/MonthlyBarChart.kt`
  - [x] 定义 `RoundedBarChartRenderer` 内部类（继承 BarChartRenderer），覆写 `drawDataSet()` 实现柱子圆角
  - [x] 定义自定义 `MarkerView`（tooltip 气泡）：圆角背景 + "X月共支出 ¥XXX.XX" 文本
  - [x] 实现 `MonthlyBarChart` Composable 函数：
    - 参数：`data: List<MonthlyExpense>`, `modifier: Modifier = Modifier`
    - 使用 `AndroidView` 嵌入 `BarChart`
    - factory 中配置：description/legend 禁用，touchEnabled=true，pinchZoom=false，背景透明
    - X轴：底部显示，drawGridLines=false，drawAxisLine=false，使用月份 label 作为 valueFormatter
    - Y轴（左侧）：drawGridLines=true，drawAxisLine=false，drawLabels=false，axisMinimum=0
    - Y轴（右侧）：禁用
    - 设置 `renderer = RoundedBarChartRenderer`
    - 设置 `marker = 自定义MarkerView`
    - update 中构建 BarDataSet：每根柱子颜色区分（当前月=primary，历史月=primary.copy(alpha=0.35f)），valueFormatter 显示 "¥XXX"，valueTextSize=10sp
    - update 中使用数据对比避免不必要的 invalidate
    - `animateY(500)`

- [x] Task 3: 在 HomeScreen 中集成月度柱状图卡片
  - [x] 在 HomeScreen.kt 的 LazyColumn 中，于"支出趋势"图表 item 之后、"当日支出分类"之前，插入新的 item block
  - [x] item 内容：
    - `HorizontalDivider`
    - `Card`（fillMaxWidth, RoundedCornerShape(16.dp), cardElevation(2.dp)）
    - `Column(padding=16.dp)` 内含标题 Text("月度支出对比", titleMedium) + Spacer(12.dp) + MonthlyBarChart
  - [x] 从 ViewModel 获取 `monthlyStats`，通过 `collectAsStateWithLifecycle()` 收集
  - [x] 注意：如果 `monthlyStats` 为空则不显示卡片（或显示空状态）

# Task Dependencies
- Task 2 依赖 Task 1（MonthlyExpense 数据类定义必须先完成）
- Task 3 依赖 Task 1 和 Task 2（需要 ViewModel 数据和图表组件就绪）