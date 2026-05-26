# Tasks

- [x] Task 1: 软删除机制与数据库迁移
  - [x] ExpenseRecord 新增 isDeleted: Boolean = false 字段
  - [x] AppDatabase 版本升级到 2，添加 Migration 1→2，ADD COLUMN is_deleted
  - [x] ExpenseDao 中所有 SELECT 查询添加 WHERE isDeleted = false
  - [x] ExpenseDao 添加软删除专用查询：softDelete(id)、getDeleted()、deleteExpired()
  - [x] MainViewModel 添加 30 天自动清理逻辑（init 时执行）

- [x] Task 2: 删除确认对话框 + 滑动改造
  - [x] RecordsScreen SwipeableRecordItem：confirmValueChange 改为弹出 AlertDialog
  - [x] 滑动到底时弹确认框而非立即调用 onDelete
  - [x] 确认后调用 viewModel.softDeleteExpense(id)
  - [x] 取消时恢复原位（回复 dismissState）
  - [x] MainViewModel 添加 softDeleteExpense 方法

- [x] Task 3: 回收站页面
  - [x] 创建 TrashScreen.kt（设置页导航进入）
  - [x] 显示 isDeleted=true 的记录列表
  - [x] 每条记录：分类、金额、平台、删除时间、恢复按钮
  - [x] "清空回收站"按钮 + 确认对话框
  - [x] SettingsScreen 新增"回收站"入口
  - [x] MainViewModel 添加恢复和真删方法

- [x] Task 4: 日期时间显示
  - [x] RecordsScreen SwipeableRecordItem 副标题改为"平台 · 渠道  |  MM-dd HH:mm"
  - [x] HomeScreen TransactionItem 副标题同步改为带日期时间
  - [x] 删除 HomeScreen 中未使用的 formatDate 函数

- [x] Task 5: 理财全选功能
  - [x] FinanceScreen 多选操作栏增加"全选"按钮
  - [x] 全选/取消全选切换逻辑
  - [x] 按钮文案动态变化

# Task Dependencies
- Task 1 是所有任务的前置依赖（先改数据库）
- Task 2 依赖 Task 1（需要 softDeleteExpense 方法）
- Task 3 依赖 Task 1（需要 getDeleted 查询）
- Task 4 可独立开发
- Task 5 可独立开发
