# Tasks

- [x] Task 1: 新建 FinanceExpense 实体类
  - 创建 `data/entity/FinanceExpense.kt`：`@Entity(tableName = "finance_expenses")`，字段 id/amount/description/fromProduct/recordedAt

- [x] Task 2: 新建 FinanceExpenseDao + FinanceExpenseRepository
  - 创建 `data/dao/FinanceExpenseDao.kt`：insert、delete、getAllExpenses(Flow)、getTotalExpenses(suspend)
  - 创建 `data/repository/FinanceExpenseRepository.kt`：封装 DAO 方法

- [x] Task 3: 更新 Room 数据库
  - 修改 `data/AppDatabase.kt`：版本升至 4，添加 FinanceExpense 实体 + MIGRATION_3_4（CREATE TABLE finance_expenses），新增 financeExpenseDao() 抽象方法
  - 修改 `di/DatabaseModule.kt`：provideFinanceExpenseDao + provideFinanceExpenseRepository

- [x] Task 4: 更新 MainViewModel
  - 注入 FinanceExpenseRepository
  - 新增 `financeExpenses: StateFlow<List<FinanceExpense>>` 状态
  - 新增 `addFinanceExpense(FinanceExpense)` / `deleteFinanceExpense(FinanceExpense)` 方法
  - 修改 `calculateFinanceExpense()` 从 `financeExpenseRepository.getTotalExpenses()` 获取

- [x] Task 5: FinanceScreen 新增收益支出记录区块
  - 在持仓明细上方新增 GlassCard 区块
  - 标题行：左侧"收益支出记录" + 右侧"+ 记录支出"按钮
  - 列表展示最近5条（description 加粗 + fromProduct 灰色小字 | -¥金额红色 + 日期）
  - "查看全部"文字按钮展开全部
  - "删除"功能（长按或滑动删除）
  - 点击"+ 记录支出"弹出 ModalBottomSheet 表单（金额/描述/来源产品下拉/日期选择/确认）
  - 监听 `financeExpenses` + `positions` StateFlow 更新 UI

- [x] Task 6: FinanceScreen 新增持仓收益排行榜
  - 在收益支出记录下方新建 GlassCard 区块
  - 标题行：左侧"收益排行" + 右侧排序切换按钮（收益率/收益额）
  - 排行列表使用 LazyColumn items + animateItemPlacement
  - RankBadge 组件（第1名金色 28dp / 第2名银色 / 第3名铜色 / 其余灰色数字）
  - 每条显示：RankBadge + 产品名+平台 + 收益额(bold)+收益率(small)

- [x] Task 7: HomeScreen 删除底部两个按钮
  - 移除 HomeScreen.kt 底部 Row 中的"添加理财"OutlinedButton 和"导出报表"OutlinedButton
  - 移除 HomeScreen 函数签名中的 `onNavigateToFinance` 和 `onExportReport` 参数
  - 移除未使用的 import（如 `clip`、`OutlinedButton` 如无其他使用）
  - 更新 MainActivity.kt 中 HomeScreen 调用处，移除不再需要的参数
  - 调整 contentPadding bottom 从 88.dp 适当减小（因为按钮行占用了约 64dp 空间）

# Task Dependencies
- Task 3 依赖 Task 1、Task 2
- Task 4 依赖 Task 2、Task 3
- Task 5 依赖 Task 4
- Task 6 依赖 Task 4（使用 positions StateFlow）
- Task 7 独立，可并行执行