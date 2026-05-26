# Tasks

- [x] Task 1: 移除 Apache POI 依赖
  - [x] 在 `app/build.gradle` 中删除 `implementation("org.apache.poi:poi:5.2.5")` 及其 exclude 块
  - [x] 在 `app/build.gradle` 中删除 `implementation("org.apache.poi:poi-ooxml:5.2.5")` 及其 exclude 块

- [x] Task 2: 创建 CsvExporter.kt
  - [x] 新建 `ui/export/CsvExporter.kt`，内部使用 `object CsvExporter`
  - [x] `generateCsv(headers, rows): String` — BOM + 双引号转义 + 逗号分隔
  - [x] `saveCsvToDownloads(context, fileName, content): Result<Uri>` — MediaStore API
  - [x] `exportExpenses(context, expenses): Result<Uri>` — 完整导出流程
  - [x] `exportPositions(context, positions): Result<Uri>` — 完整导出流程
  - [x] `createShareIntent(context, uri, subject): Intent` — 分享 Intent

- [x] Task 3: 更新 MainViewModel 添加导出数据方法
  - [x] 添加 `suspend fun getAllExpensesForExport(): List<ExpenseRecord>` — `expenseRepository.getAllExpenses().first()`
  - [x] 添加 `suspend fun getAllPositionsForExport(): List<FinancePosition>` — `financeRepository.getAllPositions().first()`

- [x] Task 4: 改造 SettingsScreen 导出 UI
  - [x] 将"导出Excel" SettingsRow 替换为两个导出按钮区域
  - [x] "导出支出记录(CSV)"按钮 → 调用 CsvExporter.exportExpenses + 分享
  - [x] "导出理财持仓(CSV)"按钮 → 调用 CsvExporter.exportPositions + 分享
  - [x] 导出中显示"正在导出..."文字，禁用按钮
  - [x] 导出成功显示绿色提示 + 分享按钮
  - [x] 导出失败显示错误原因
  - [x] 移除 `ExcelExporter` 导入

- [x] Task 5: 清理旧文件
  - [x] 已删除 ExcelExporter.kt（Apache POI 方案已完全替代）

# Task Dependencies
- Task 2 可独立执行
- Task 4 依赖 Task 2 和 Task 3
- Task 1 和 Task 5 可与其他任务并行