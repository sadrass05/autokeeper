# 首页与理财功能增强 Spec

## Why
首页"添加理财"和"导出报表"按钮无响应，支出趋势图使用假数据，UI仅支持深色主题缺乏切换能力，理财界面未展示理财收益支出与净收益的完整计算逻辑。

## What Changes
- 首页按钮接入实际功能（添加理财导航至理财页、导出报表生成Excel文件）
- 支出趋势图替换为真实数据库数据，X轴显示日期标签，新增当日支出分类圆环图
- 支持深色/浅色双主题切换，设置页增加主题切换入口，偏好持久化
- 理财页增加理财收益支出展示和剩余净收益计算，支出记录中理财支出有特殊标记和专用分类

## Impact
- Affected specs: 无
- Affected code: HomeScreen.kt, MiniLineChart.kt, Theme.kt, Color.kt, MainActivity.kt, SettingsScreen.kt, FinanceScreen.kt, RecordsScreen.kt, MainViewModel.kt, ExpenseDao.kt

---

## ADDED Requirements

### Requirement: 首页按钮功能
首页底部的"添加理财"按钮 SHALL 导航至理财Tab页面，"导出报表"按钮 SHALL 触发Excel导出并保存到设备。

#### Scenario: 点击添加理财
- **WHEN** 用户在首页点击"添加理财"按钮
- **THEN** 应用切换到理财Tab（index=2），展示FinanceScreen

#### Scenario: 点击导出报表
- **WHEN** 用户在首页点击"导出报表"按钮
- **THEN** 系统生成包含所有支出记录的Excel文件，通过ShareSheet或保存到下载目录

### Requirement: 真实数据支出趋势图
首页支出趋势图 SHALL 使用数据库中最近7天的实际支出数据，X轴 SHALL 显示对应日期标签。

#### Scenario: 展示真实支出趋势
- **WHEN** 首页加载
- **THEN** 支出趋势图显示最近7天（含今天）的每日支出金额
- **AND** X轴标签显示 "MM-dd" 格式的日期
- **AND** 若某天无支出记录，该天金额为0

#### Scenario: 数据更新
- **WHEN** 有新的支出记录被添加
- **THEN** 趋势图自动刷新以反映最新数据

### Requirement: 当日支出分类圆环图
首页 SHALL 在支出趋势图下方展示当日支出按分类的圆环图（Donut Chart）。

#### Scenario: 展示分类支出
- **WHEN** 首页加载且有当日支出数据
- **THEN** 圆环图按分类（餐饮、交通、购物等）以不同颜色展示支出占比
- **AND** 每个扇区旁显示分类名称和金额

#### Scenario: 当日无支出
- **WHEN** 当日没有任何支出记录
- **THEN** 圆环图区域显示"今日暂无支出"占位文本

### Requirement: 双主题切换
应用 SHALL 支持深色主题和浅色主题，用户可在设置页切换，偏好 SHALL 持久化存储。

#### Scenario: 默认深色主题
- **WHEN** 应用首次启动
- **THEN** 默认使用深色主题

#### Scenario: 切换到浅色主题
- **WHEN** 用户在设置页选择浅色主题
- **THEN** 整个应用UI切换为浅色配色方案
- **AND** 所有页面（首页、记录、理财、设置）即时响应
- **AND** 偏好通过DataStore持久化，下次启动保持选择

#### Scenario: 切换回深色主题
- **WHEN** 用户在设置页选择深色主题
- **THEN** 应用恢复深色配色方案

### Requirement: 理财收益支出与净收益
理财页 SHALL 展示"理财收益支出"总额（isFinanceExpense=true的记录），并计算"剩余净收益 = 理财收益 - 理财收益支出"。

#### Scenario: 理财页展示完整收益信息
- **WHEN** 用户进入理财Tab
- **THEN** 页面展示4列概览：总市值、累计收益、理财收益支出、剩余净收益
- **AND** 剩余净收益 = 累计收益 - 理财收益支出

#### Scenario: 无理财支出时
- **WHEN** 没有标记为理财支出的记录
- **THEN** 理财收益支出显示¥0.00，剩余净收益等于累计收益

### Requirement: 理财支出特殊标记与分类
支出记录中标记为理财支出的记录 SHALL 使用专用分类"理财支出"，在记录列表中有视觉区分，且不计入总支出统计。

#### Scenario: 标记支出为理财支出
- **WHEN** 用户在记录编辑中将某条支出标记为"理财支出"
- **THEN** 该记录的isFinanceExpense设为true，category设为"理财支出"
- **AND** 记录列表中该条目显示特殊标记（如标签或图标）

#### Scenario: 理财支出不计入总支出
- **WHEN** 系统计算今日支出、本月支出、总支出
- **THEN** isFinanceExpense=true的记录不参与计算

## MODIFIED Requirements

### Requirement: 首页支出概览（修改）
**原行为**：首页Hero区域显示"今日支出"、"本月支出"、"理财收益"三列。
**修改后**：保持三列布局不变，但"本月支出"和"今日支出"的计算需排除isFinanceExpense=true的记录。

### Requirement: 理财页概览（修改）
**原行为**：理财页展示3列概览（总市值、累计收益、收益率）。
**修改后**：扩展为4列概览（总市值、累计收益、理财收益支出、剩余净收益），移除原收益率列。