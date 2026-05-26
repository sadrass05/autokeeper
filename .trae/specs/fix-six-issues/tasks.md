# Tasks

- [x] Task 1: MiniLineChart 主题响应修复
  - [x] 将 X 轴 textColor 设置从 factory 移到 update 闭包
  - [x] update 闭包中同步 gridColor、lineColor、fillColor、textColor
  - [x] 验证深色/浅色切换时标签颜色正确

- [x] Task 2: 手动添加支出功能
  - [x] RecordsScreen 添加"手动添加"按钮
  - [x] 创建 AddExpenseSheet（ModalBottomSheet）：金额、分类、平台、支付渠道、理财支出标记
  - [x] MainViewModel 添加 addExpense 方法
  - [x] 保存后通过 ViewModel 插入 Room 并刷新列表

- [x] Task 3: 记录列表显示调整
  - [x] SwipeableRecordItem 主标题从 expense.merchant 改为 expense.category
  - [x] 保留平台副标题，理财标记位置调整到分类标签旁
  - [x] 首页最近交易列表同步调整

- [x] Task 4: 理财持仓文件导入
  - [x] 创建 FinanceImportParser.kt：表格解析 + 自由文本解析
  - [x] FinanceScreen 添加导入按钮（概览卡片右上角 IconButton）
  - [x] 接入文件选择器，解析后批量插入 Room
  - [x] MainViewModel 添加 addFinancePositions 批量方法
  - [x] Toast 显示导入结果

- [x] Task 5: Flask 后端创建
  - [x] 创建 backend/app.py，Flask + PyMySQL
  - [x] 实现 4 个 API 端点（expenses/sync、expenses/list、finance/sync、finance/list）
  - [x] 创建 backend/requirements.txt
  - [x] CORS 配置，MySQL 连接参数

- [x] Task 6: SettingsScreen 按钮接线
  - [x] "数据库同步" onClick → 接入 MySqlApi 推送数据
  - [x] "导出Excel" onClick → 调用 ExcelExporter
  - [x] 两个操作都通过协程 IO 线程执行，Toast 提示结果

- [x] Task 7: Excel 导出线程修复
  - [x] ExcelExporter.exportToExcel 内部用 withContext(Dispatchers.IO) 包裹
  - [x] MainActivity 导出调用改用 Dispatchers.IO

- [x] Task 8: 回归验证
  - [x] 验证理财支出 Switch 自动分类功能正常
  - [x] 验证所有页面的主题切换正常
  - [x] 验证导入导出流程端到端正常

# Task Dependencies
- Task 1 可独立开发
- Task 2 和 Task 3 可并行（都在 RecordsScreen.kt），建议同一 agent 完成
- Task 4 可独立开发
- Task 5 可独立开发（纯后端）
- Task 6 依赖 Task 5（后端先创建），依赖 Task 7（使用修复后的 Exporter）
- Task 7 可独立开发
- Task 8 在所有 Task 完成后统一验证