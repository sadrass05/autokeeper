# Tasks

- [x] Task 1: TrendChart.kt update lambda 添加数据相等性检查
  - [x] SubTask 1.1: LineChart update：计算 newDataSet + newData，与 chart.data 比较后决定是否 invalidate
  - [x] SubTask 1.2: BarChart update：同上
  - [x] SubTask 1.3: 颜色 Argb 值用 remember 包裹

- [x] Task 2: HomeScreen.kt 图表 item 添加稳定 key
  - [x] SubTask 2.1: `item { }` 改为 `item(key = "expense_chart") { }`

# Task Dependencies
- Tasks 1, 2 相互独立，可并行执行