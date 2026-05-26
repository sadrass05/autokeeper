# Tasks

- [x] Task 1: 在 MainViewModel 中新增 topExpenses StateFlow
  - [x] 导入 `kotlinx.coroutines.flow.SharingStarted` 和 `kotlinx.coroutines.flow.stateIn`
  - [x] 新增 `val topExpenses: StateFlow<List<ExpenseRecord>>`，从 `expenses` flow 派生
  - [x] 过滤逻辑：`recordCal.YEAR == currentYear && recordCal.MONTH == currentMonth`
  - [x] 排序 + 截取：`.sortedByDescending { it.amount }.take(5)`
  - [x] 使用 `.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())`

- [x] Task 2: 创建 TopExpensesCard.kt 组件
  - [x] 新建 `app/src/main/java/com/example/autobookkeeper/ui/components/TopExpensesCard.kt`
  - [x] 实现 `RankBadge` 子组件：根据 rank 渲染3种样式（1/2-3/4-5）
  - [x] 实现 `CategoryIconCircle` 子组件：40dp圆形 + 分类首字符 + 根据分类名映射颜色
  - [x] 实现 `formatRecordDate` 工具函数：时间戳 → "MM-dd HH:mm"
  - [x] 实现 `getCategoryColor` 工具函数：分类名 → Color（10色映射+5色fallback）
  - [x] 实现 `TopExpensesCard` Composable：
    - 参数：`expenses: List<ExpenseRecord>`, `onViewAll: () -> Unit`, `modifier: Modifier`
    - 使用 `Card` 容器（RoundedCornerShape(16.dp), cardElevation=2dp）
    - 内部 `Column(padding=16.dp)` 排列：
      1. 标题 Text("本月支出排行", titleMedium)
      2. Spacer(12.dp)
      3. 每条记录的 Row 布局（序号 + 图标圆 + 信息列 + 金额日期列）
      4. Spacer(8.dp)
      5. "查看全部记录 →" 按钮（TextButton, 右对齐）
    - 空数据: `if (expenses.isEmpty()) return`

- [x] Task 3: 在 HomeScreen 中添加 onNavigateToRecords 回调并插入卡片
  - [x] HomeScreen 函数签名新增 `onNavigateToRecords: () -> Unit = {}`
  - [x] 从 ViewModel 收集 `topExpenses`
  - [x] 在 LazyColumn 的月度对比图表 item 之后、当日支出分类 item 之前插入新 item
  - [x] 新 item 包含 `TopExpensesCard(expenses = topExpenses, onViewAll = onNavigateToRecords)`
  - [x] 当 topExpenses 非空时渲染（空时跳过 — 由组件内部处理）

- [x] Task 4: 在 MainActivity 中传递 onNavigateToRecords 回调
  - [x] 在 HomeScreen 调用处添加 `onNavigateToRecords = { selectedScreen = 1 }`

# Task Dependencies
- Task 2 依赖 Task 1（需要 topExpenses 数据类型确认）
- Task 3 依赖 Task 2（需要 TopExpensesCard 组件就绪）
- Task 4 依赖 Task 3（需要 HomeScreen 已声明回调参数）