# Tasks

- [x] Task 1: 修复导出闪退
  - [x] proguard-rules.pro 增加 `-keep class org.openxmlformats.schemas.** { *; }` 和 `-dontwarn`
  - [x] ExcelExporter.kt catch(Exception) 改为 catch(Throwable)
  - [x] MediaStore 写入增加 IS_PENDING 标记逻辑
  - [x] 验证 APK 构建成功，导出功能在真机正常运行

- [x] Task 2: 支出记录金额加粗放大
  - [x] RecordsScreen.kt SwipeableRecordItem 金额样式改为 headlineLarge + Bold
  - [x] HomeScreen.kt TransactionItem 金额样式改为 headlineMedium + Bold

- [x] Task 3: 理财多选批量删除
  - [x] FinanceScreen 增加多选模式状态管理（isMultiSelectMode, selectedIds）
  - [x] 标题栏旁增加"多选"按钮，多选模式下顶部显示操作栏
  - [x] 每行左侧增加 Checkbox（多选模式下可见）
  - [x] "删除"按钮批量调用 viewModel.deleteFinancePositions(ids)
  - [x] MainViewModel 添加 deleteFinancePositions 批量方法
  - [x] FinanceDao 添加 deleteByIds 批量删除查询

- [x] Task 4: 持仓长按菜单（加仓/减仓/删除）
  - [x] 长按持仓条目弹出 DropdownMenu：加仓、减仓、删除
  - [x] 加仓弹出 AlertDialog 输入金额，更新 buyAmount 和 currentValue
  - [x] 减仓弹出 AlertDialog 输入金额，减少 buyAmount 和 currentValue
  - [x] 删除确认后调用现有 deleteFinancePosition
  - [x] MainViewModel 添加 updateFinancePosition 方法

- [x] Task 5: 同产品自动合并
  - [x] FinanceDao 添加 getByName 方法
  - [x] FinanceRepository 添加 upsertPosition 方法（按 productName 匹配合并）
  - [x] 导入流程（FinanceImportParser）和手动添加（AddFinanceSheet）接入 upsertPosition
  - [x] MainViewModel.addFinancePosition 和 addFinancePositions 改为使用 upsert

# Task Dependencies
- Task 1 可独立开发
- Task 2 可独立开发
- Task 3 和 Task 4 可并行开发（都在 FinanceScreen.kt）
- Task 5 依赖 Task 3+4 完成后的 FinanceScreen 上下文，但 DAO/Repository 层可提前开发