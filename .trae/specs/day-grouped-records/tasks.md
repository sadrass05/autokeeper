# Tasks

- [x] Task 1: 创建 CategoryCircleIcon.kt 组件
  - [x] 新建 `app/src/main/java/com/example/autobookkeeper/ui/components/CategoryCircleIcon.kt`
  - [x] 定义 `getCategoryColor(category: String): Color` 函数：8 种预设颜色 + default 灰色
  - [x] 实现 `CategoryCircleIcon` Composable：
    - 参数：`category: String`, `size: Dp = 40.dp`, `modifier: Modifier = Modifier`
    - 渲染：`Box(size=size, CircleShape clip, bgColor) + Text(category.first(), white, Bold, centered)`

- [x] Task 2: 创建 DayGroupHeader.kt 组件
  - [x] 新建 `app/src/main/java/com/example/autobookkeeper/ui/components/DayGroupHeader.kt`
  - [x] 定义 `isSameDay(today, target): Boolean` 工具函数
  - [x] 定义 `isYesterday(today, target): Boolean` 工具函数（前一天）
  - [x] 定义 `formatGroupDate(dateMillis: Long): String` 函数：`M月d日` + 今天/昨天/星期X
  - [x] 实现 `DayGroupHeader` Composable：
    - 参数：`dateMillis: Long`, `totalOut: Double`, `totalIn: Double`, `modifier: Modifier`
    - 渲染：Row（spaceBetween）→ 左侧 formatGroupDate 结果 + 右侧收支摘要

- [x] Task 3: 重构 RecordsScreen.kt — 提取 ExpenseRecordItem + 分组 LazyColumn
  - [x] 导入新增的 CategoryCircleIcon 和 DayGroupHeader
  - [x] 新增 `groupedExpenses` 计算：`filteredExpenses.groupBy { SimpleDateFormat("yyyy-MM-dd").format(Date(it.recordedAt)) }`
  - [x] 新增 `sortedDates`：`groupedExpenses.keys.sortedDescending()`
  - [x] 替换现有 `items(filteredExpenses)` 为分组结构：
    - 外层遍历 `sortedDates`
    - 每个日期：`stickyHeader(key="header_$dateKey") { DayGroupHeader }` + `items(dayExpenses.sortedByDescending{it.recordedAt}, key={it.id}) { ExpenseRecordItem }`
  - [x] 提取 `SwipeableRecordItem` 内部 Row 为独立的 `ExpenseRecordItem` Composable：
    - 显示：分类图标圆（40dp）+ 时间/商户信息列（15sp Medium 分类 + 12sp 时间|商户）+ 金额（支出红/收入绿）
  - [x] 每条记录包裹 SwipeToDismissBox（保留现有逻辑）
  - [x] 组内记录间添加 `HorizontalDivider(padding(start=68.dp), thickness=0.5dp)`
  - [x] 保留所有现有功能：FilterBar、AddExpenseSheet、EditRecordSheet、删除确认对话框
  - [x] 移除 LazyColumn 的 `verticalArrangement = spacedBy(6.dp)`（改用 divider 分隔）

- [x] Task 4: 清理 RecordsScreen.kt 未使用的导入
  - [x] 移除不再需要的导入（移除了 `Surface`）
  - [x] 添加新增的导入（已添加：`CategoryCircleIcon`, `DayGroupHeader`, `stickyHeader`, `TextOverflow`, `sp`）

# Task Dependencies
- Task 3 依赖 Task 1 和 Task 2（需要 CategoryCircleIcon 和 DayGroupHeader 组件就绪）
- Task 4 在 Task 3 之后执行（重构后清理导入）