# CSV 导出替代 Apache POI Spec

## Why
当前使用 Apache POI 导出 Excel 闪退，原因是 POI 在 Android 上依赖不可用的 Java 类、内存占用大导致 OOM、文件权限复杂。改为纯 Kotlin 实现的 CSV 格式导出，无第三方依赖，Excel 可直接打开。

## What Changes
- **新建** `ui/export/CsvExporter.kt` — 替代 ExcelExporter.kt，纯 Kotlin 实现
- **删除/弃用** `ui/export/ExcelExporter.kt` 中的 Apache POI 代码
- **修改** `ui/screen/SettingsScreen.kt` — 将"导出Excel"拆为两个 CSV 导出按钮，导出完成提供分享
- **修改** `app/build.gradle` — 移除 Apache POI 依赖
- **修改** `ui/viewmodel/MainViewModel.kt` — 添加 `getAllExpensesForExport()` 和 `getAllPositionsForExport()`

## Impact
- Affected specs: `fix-export-finance-records`（该 spec 基于 POI，CSV 方案完全替代之）
- Affected code:
  - `ui/export/ExcelExporter.kt` → `ui/export/CsvExporter.kt`
  - `ui/screen/SettingsScreen.kt`
  - `app/build.gradle`
  - `ui/viewmodel/MainViewModel.kt`

## ADDED Requirements

### Requirement: CSV 支出记录导出
系统 SHALL 支持导出支出记录为 CSV 文件（UTF-8 BOM），表头为"日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出"，数据按时间从新到旧排列。

#### Scenario: 导出成功
- **GIVEN** 本地有 20 条支出记录
- **WHEN** 用户点击"导出支出记录(CSV)"
- **THEN** CSV 文件保存到 Downloads，Snackbar 显示"已导出到下载文件夹"，并提供分享按钮

#### Scenario: 导出失败
- **GIVEN** 存储空间不足或权限问题
- **WHEN** 用户点击导出
- **THEN** 显示具体错误原因

### Requirement: CSV 理财持仓导出
系统 SHALL 支持导出理财持仓为 CSV 文件，表头为"产品名称,平台,买入金额,当前市值,收益,收益率"。

### Requirement: MediaStore 保存
系统 SHALL 使用 MediaStore API 保存 CSV 到 Downloads 文件夹，兼容 Android 10+，无需 WRITE_EXTERNAL_STORAGE 权限。

### Requirement: CSV 字符转义
系统 SHALL 正确处理 CSV 中双引号转义（`"` → `""`）和 BOM 头（`\uFEFF`）确保 Excel 正确识别中文。

### Requirement: 分享功能
系统 SHALL 在导出成功后提供 ACTION_SEND 分享 Intent，用户可将 CSV 分享至微信、邮件等。

### Requirement: 移除 POI 依赖
系统 SHALL 从 build.gradle 中移除 `org.apache.poi:poi` 和 `org.apache.poi:poi-ooxml` 依赖。

### Requirement: MainViewModel 导出数据方法
系统 SHALL 在 MainViewModel 中提供 `getAllExpensesForExport()` 和 `getAllPositionsForExport()` 方法，通过 Repository 的 `first()` 从 Flow 获取完整数据列表。

## REMOVED Requirements

### Requirement: Apache POI Excel 导出
**Reason**: POI 在 Android 上不稳定，OOM/类缺失导致闪退
**Migration**: 全部替换为 CSV 导出，Excel 可直接打开 CSV 文件