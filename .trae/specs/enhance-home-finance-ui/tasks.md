# Tasks

- [x] Task 1: 实现深色/浅色双主题切换系统
  - [x] 在Color.kt中添加浅色主题颜色定义（LightBackground, LightSurface等）
  - [x] 在Theme.kt中创建LightColorScheme，修改AutoBookkeeperTheme支持参数化主题
  - [x] 创建ThemePrefs DataStore工具类用于持久化主题偏好
  - [x] 在MainActivity中管理主题状态，读取持久化偏好并应用
  - [x] 更新所有Screen和Component中使用硬编码DarkXxx颜色的地方改为MaterialTheme.colorScheme引用

- [x] Task 2: 首页按钮功能接入
  - [x] HomeScreen接收导航回调参数(onNavigateToFinance, onExportReport)
  - [x] "添加理财"按钮onClick调用onNavigateToFinance
  - [x] "导出报表"按钮onClick调用onExportReport，触发Excel导出逻辑
  - [x] MainActivity传递导航回调给HomeScreen（切换selectedScreen=2，调用导出方法）

- [x] Task 3: 真实数据支出趋势图 + 日期标签
  - [x] 在ExpenseDao中添加getDailyExpenseForLast7Days查询（按天分组、排除理财支出）
  - [x] 在ExpenseRepository中添加对应方法
  - [x] 在MainViewModel中添加trendData StateFlow，加载最近7天每日支出
  - [x] 修改MiniLineChart支持传入日期标签列表，X轴显示"MM-dd"格式日期
  - [x] HomeScreen从ViewModel读取trendData并传入MiniLineChart

- [x] Task 4: 当日支出分类圆环图
  - [x] 创建CategoryDonutChart Compose组件（Canvas绘制圆环图）
  - [x] 在ExpenseDao中添加getTodayExpenseByCategory查询
  - [x] 在ExpenseRepository和MainViewModel中添加对应数据流
  - [x] 在HomeScreen支出趋势图下方嵌入CategoryDonutChart
  - [x] 当日无支出时显示占位文本

- [x] Task 5: 理财收益支出与净收益展示
  - [x] 在MainViewModel中添加financeExpense StateFlow（汇总isFinanceExpense=true的记录）
  - [x] 在MainViewModel中添加netFinanceProfit StateFlow（理财收益 - 理财收益支出）
  - [x] FinanceScreen概览区域改为4列：总市值、累计收益、理财收益支出、剩余净收益
  - [x] 更新净收益公式为：剩余净收益 = 累计收益 - 理财收益支出

- [x] Task 6: 理财支出特殊标记与专用分类
  - [x] 在CategoryRepository中添加"理财支出"默认分类
  - [x] RecordsScreen中理财支出条目显示特殊标记（如金色标签"理财"）
  - [x] 编辑记录时支持切换isFinanceExpense标记，同步更新category为"理财支出"
  - [x] HomeScreen中今日/本月支出确认使用isFinanceExpense=false过滤

- [x] Task 7: Excel导出功能实现
  - [x] 创建ExcelExporter工具类，使用Apache POI生成Excel文件
  - [x] 导出所有支出记录（含理财支出标记列），使用MediaStore保存到下载目录
  - [x] 导出完成后通过Toast或Snackbar通知用户文件路径

- [x] Task 8: 主题适配全页面
  - [x] 确保HomeScreen、RecordsScreen、FinanceScreen、SettingsScreen全部使用MaterialTheme.colorScheme
  - [x] MiniLineChart和CategoryDonutChart颜色适配主题
  - [x] NavigationBar颜色适配主题
  - [x] 浅色主题下图表颜色、分割线颜色调整

# Task Dependencies
- Task 1 (主题系统) 是所有UI任务的前置依赖，应最先完成
- Task 8 (主题适配) 应在Task 2-6完成后统一检查
- Task 2 (按钮功能) 依赖MainActivity的导航架构
- Task 3 (趋势图) 和 Task 4 (圆环图) 可并行开发
- Task 5 (理财净收益) 和 Task 6 (理财支出标记) 可并行开发
- Task 7 (Excel导出) 可独立开发