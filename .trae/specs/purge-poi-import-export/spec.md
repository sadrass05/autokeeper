# 彻底移除 Apache POI，全面重写导入导出 Spec

## Why
`csv-export-replacement` 仅替换了导出端，ImportManager.kt 和 FinanceScreen.kt 中仍大量引用 POI API（WorkbookFactory、Cell、Sheet 等），导致编译错误且无法运行。需要彻底清理所有 POI 痕迹，统一为纯 Android 原生方案。

## What Changes
- **删除** `ui/export/ExcelExporter.kt` — POI Excel 导出
- **新建** `ui/export/CsvExporter.kt` — Hilt @Singleton 注入的 CSV 导出器（替代之前 object 版本）
- **重写** `ui/importdata/ImportManager.kt` — 移除 POI，仅支持 CSV + TXT 导入
- **修改** `data/dao/ExpenseDao.kt` — 新增 `getAllExpensesList()` 一次性查询
- **修改** `data/dao/FinanceDao.kt` — 新增 `getAllPositionsList()` 一次性查询
- **修改** `data/repository/ExpenseRepository.kt` — 新增 `getAllExpensesOnce()`
- **修改** `data/repository/FinanceRepository.kt` — 新增 `getAllPositionsOnce()`
- **修改** `ui/screen/FinanceScreen.kt` — 删除 POI Excel 导入函数（getCellString、importExcelToFinance 等）
- **修改** `ui/screen/SettingsScreen.kt` — 适配新的 CsvExporter API（ExportResult sealed class）
- **修改** `app/build.gradle` — 确保 POI 依赖已移除（清理残留）

## Impact
- Affected specs: `csv-export-replacement`（完全替代）、`fix-export-finance-records`（完全替代）
- Affected code: 9 个文件（见上方列表）

## ADDED Requirements

### Requirement: Hilt 注入 CsvExporter
系统 SHALL 将 CsvExporter 实现为 @Singleton 类，通过 @Inject 构造函数注入 ExpenseRepository 和 FinanceRepository，直接调用 DAO 获取全量数据。

### Requirement: DAO 一次性查询
系统 SHALL 在 ExpenseDao 和 FinanceDao 中新增 `suspend` 方法返回 List（非 Flow），供导出使用。

### Requirement: ImportManager 去 POI
系统 SHALL 重写 ImportManager，移除所有 POI 代码（WorkbookFactory、Cell、Sheet），仅支持 CSV 和 TXT 格式，解析 CSV 时处理引号转义。

### Requirement: CSV 行解析
系统 SHALL 在 ImportManager 中实现 `parseCsvLine()` 方法，正确处理引号包裹的字段（如 `"包含,逗号"` 的值）。

### Requirement: 导入文件格式检查
系统 SHALL 根据文件扩展名和 MIME type 判断文件类型，支持的格式：.csv、.txt。

### Requirement: FinanceScreen POI 清理
系统 SHALL 删除 FinanceScreen.kt 中所有引用 org.apache.poi 的函数和 import。

### Requirement: 导出结果类型
系统 SHALL 使用 `sealed class ExportResult` 封装导出结果，Success 包含 fileName、uri、count，Failure 包含 error。

## REMOVED Requirements

### Requirement: Excel 导入（POI 方案）
**Reason**: Android 不支持 POI 的完整运行时
**Migration**: 替换为 CSV + TXT 纯文本导入

### Requirement: object CsvExporter
**Reason**: 需要 Repository 依赖注入
**Migration**: 替换为 Hilt @Singleton class