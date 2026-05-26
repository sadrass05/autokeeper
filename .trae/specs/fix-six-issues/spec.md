# App 六项改进 Spec

## Why
MiniLineChart 深色模式下 X 轴标签可能与背景同色、RecordsScreen 无法手动添加支出、理财页缺少持仓导入、SettingsScreen 同步/导出按钮无响应、Excel 导出在主线程执行导致闪退。

## What Changes
- MiniLineChart X 轴标签颜色在主题切换时实时响应
- RecordsScreen 增加手动添加支出功能，列表标题改为分类名
- FinanceScreen 增加文件导入持仓数据（表格 + 自由文本双格式）
- 创建 Flask 后端 MySQL 同步服务，SettingsScreen 接线
- ExcelExporter 切换到 IO 线程，修复闪退
- 回归验证理财标记自动分类功能

## Impact
- Affected specs: enhance-home-finance-ui
- Affected code: MiniLineChart.kt, RecordsScreen.kt, FinanceScreen.kt, SettingsScreen.kt, ExcelExporter.kt, MainActivity.kt, MainViewModel.kt
- New files: backend/app.py, backend/requirements.txt, ui/importdata/FinanceImportParser.kt

---

## ADDED Requirements

### Requirement: X 轴标签主题响应
MiniLineChart 的 X 轴标签颜色 SHALL 在深色/浅色主题切换时实时更新。

#### Scenario: 主题切换时标签颜色跟随
- **WHEN** 用户在设置页切换深色/浅色主题
- **THEN** 首页支出趋势图 X 轴日期标签颜色自动适配当前主题
- **AND** 深色模式使用浅色文本，浅色模式使用深色文本

### Requirement: 手动添加支出
RecordsScreen SHALL 提供手动添加支出记录的功能入口。

#### Scenario: 手动添加一笔支出
- **WHEN** 用户点击"手动添加"按钮
- **THEN** 弹出表单（金额、分类、平台、支付渠道、理财支出标记）
- **AND** 保存后记录插入 Room 并刷新列表

### Requirement: 记录列表显示分类
支出记录列表 SHALL 以分类名作为主标题，平台信息作为副标题。

#### Scenario: 列表显示
- **WHEN** 查看支出记录列表
- **THEN** 每条记录显示分类名（如"餐饮"）为主标题
- **AND** 副标题显示"平台 · 支付渠道"

### Requirement: 理财持仓文件导入
FinanceScreen SHALL 支持从 TXT/Excel 文件导入持仓数据，支持表格格式和自由文本格式。

#### Scenario: 表格格式导入
- **WHEN** 用户选择包含表格格式持仓数据的文件
- **THEN** 系统按逗号/制表符解析每行，提取产品名称、平台、买入金额、当前市值、收益、收益率
- **AND** 解析成功的记录批量插入 Room

#### Scenario: 自由文本导入
- **WHEN** 表格解析失败时
- **THEN** 回退到自由文本解析，正则匹配金额和产品名关键字
- **AND** 显示成功/失败数量

### Requirement: MySQL 同步后端
系统 SHALL 提供 Flask 后端服务，接收 App 推送的数据并写入 MySQL。

#### Scenario: 同步支出数据
- **WHEN** 用户在设置页点击"数据库同步"
- **THEN** App 通过 Retrofit 将本地支出和理财数据推送到 Flask 后端
- **AND** 后端写入 MySQL，返回成功确认

### Requirement: 设置页按钮接线
SettingsScreen 的"数据库同步"和"导出Excel"按钮 SHALL 执行对应功能。

#### Scenario: 点击数据库同步
- **WHEN** 用户点击"数据库同步"
- **THEN** 调用 MySqlApi 推送数据，Toast 提示结果

#### Scenario: 点击导出Excel
- **WHEN** 用户点击"导出Excel"
- **THEN** 调用 ExcelExporter 生成文件，Toast 提示路径

## MODIFIED Requirements

### Requirement: Excel 导出线程安全
**原行为**：ExcelExporter.exportToExcel 在调用方协程中同步执行 I/O，默认在主线程。
**修改后**：exportToExcel 内部使用 withContext(Dispatchers.IO) 包裹所有操作，调用方使用 Dispatchers.IO。
