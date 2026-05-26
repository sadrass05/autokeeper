# Tasks

- [x] Task 1: 确认/执行 build.gradle POI 清理
  - [x] 确保 `org.apache.poi:poi` 和 `org.apache.poi:poi-ooxml` 依赖已删除

- [x] Task 2: DAO 新增一次性查询方法
  - [x] ExpenseDao 新增 `@Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY recordedAt DESC") suspend fun getAllExpensesList(): List<ExpenseRecord>`
  - [x] FinanceDao 新增 `@Query("SELECT * FROM finance_positions") suspend fun getAllPositionsList(): List<FinancePosition>`

- [x] Task 3: Repository 新增一次性查询方法
  - [x] ExpenseRepository 新增 `suspend fun getAllExpensesOnce()` 调用 `expenseDao.getAllExpensesList()`
  - [x] FinanceRepository 新增 `suspend fun getAllPositionsOnce()` 调用 `financeDao.getAllPositionsList()`

- [x] Task 4: 重写 CsvExporter.kt（Hilt 注入版本）
  - [x] 删除现有的 object CsvExporter，替换为 @Singleton class
  - [x] 构造函数注入 ExpenseRepository + FinanceRepository
  - [x] `buildCsv(headers, rows): String` — BOM + 双引号转义
  - [x] `saveToDownloads(context, fileName, content): Result<Uri>` — MediaStore API + IS_PENDING
  - [x] `exportExpenses(context): ExportResult` — 调用 `expenseRepository.getAllExpensesOnce()`
  - [x] `exportPositions(context): ExportResult` — 调用 `financeRepository.getAllPositionsOnce()`
  - [x] `sealed class ExportResult { Success(fileName, uri, count) ; Failure(error) }`

- [x] Task 5: 重写 ImportManager.kt（移除 POI）
  - [x] 删除所有 POI 相关代码（importExcel、getCellString、WorkbookFactory 引用）
  - [x] 保留 importTxt（原样保留，逻辑不变）
  - [x] 新增 importCsv：解析 CSV 格式（含引号转义解析 parseCsvLine）
  - [x] `parseCsvLine(line): List<String>` — 处理引号包裹的字段
  - [x] `readLines(context, uri): List<String>` — 读取文件所有行
  - [x] `readContent(context, uri): String` — 读取文件全文
  - [x] `getFileName(context, uri): String` — 查询文件名
  - [x] `parseAndInsert(lines, parse)` — 通用解析→插入流程
  - [x] `importFile(context, uri): ImportResult` — 入口方法，按扩展名分发
  - [x] 删除 `org.apache.poi` 相关 import

- [x] Task 6: 清理 FinanceScreen.kt POI 代码
  - [x] 删除 `import org.apache.poi.ss.usermodel.WorkbookFactory`
  - [x] 删除 `getCellString()` 函数（约475-490行）
  - [x] 删除 `importExcelToFinance()` 函数（约492-530行）
  - [x] 删除"文件导入"按钮中对 Excel MIME type 的引用（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）
  - [x] 保留 CSV/TXT 导入入口不变

- [x] Task 7: 适配 SettingsScreen.kt 导出 UI
  - [x] 将 CsvExporter 调用从 object 静态方法改为 Hilt 注入实例
  - [x] 适配新的 ExportResult sealed class 返回值
  - [x] 导出成功：显示"已导出 X 条记录到下载文件夹：文件名" + 分享按钮
  - [x] 导出失败：显示错误信息
  - [x] 确保 `CsvExporter` 通过 EntryPoint 注入（同 SyncPrefs 模式）

- [x] Task 8: 删除旧 ExcelExporter.kt
  - [x] 确认 ExcelExporter.kt 已不存在（如存在则删除）

# Task Dependencies
- Task 4 依赖 Task 2 + Task 3（CsvExporter 需要 Repository 的 getAllXxxOnce）
- Task 5 可独立执行
- Task 6 可独立执行
- Task 7 依赖 Task 4（SettingsScreen 需要新的 CsvExporter API）
- Task 8 可独立执行