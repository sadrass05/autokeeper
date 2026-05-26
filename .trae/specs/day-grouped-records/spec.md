# 交易记录按天分组显示 Spec

## Why
RecordsScreen 当前使用平面列表展示所有交易记录，缺少日期分组上下文。用户需要像主流记账 App 一样看到按天分组、每天有收支摘要、一目了然的明细列表。

## What Changes
- 新增 `CategoryCircleIcon.kt` 组件 — 40dp 彩色圆形分类图标
- 新增 `DayGroupHeader.kt` 组件 — sticky header 显示日期 + 收支摘要
- **重构** `RecordsScreen.kt` — 从平面 LazyColumn 改为 stickyHeader 分组结构
- 提取记录行内联代码为独立的 `ExpenseRecordItem` 组件
- 保留现有 FilterBar（平台/分类/月份）和 SwipeToDismissBox 左滑删除
- 保留 AddExpenseSheet 和 EditRecordSheet

## Impact
- Affected specs: 无
- Affected code:
  - **新增**: `ui/components/CategoryCircleIcon.kt`
  - **新增**: `ui/components/DayGroupHeader.kt`
  - **重构**: `ui/screen/RecordsScreen.kt`（LazyColumn 结构 + 内联组件提取）

## ADDED Requirements

### Requirement: CategoryCircleIcon 组件
系统 SHALL 提供 `CategoryCircleIcon` Composable，接收分类名和尺寸参数，渲染指定尺寸的圆形图标，背景色根据分类名映射，中央显示分类首字符。

#### Scenario: 颜色映射
- **GIVEN** 分类为"餐饮"
- **WHEN** 渲染 CategoryCircleIcon
- **THEN** 显示 40dp 绿色圆形 + 白色"餐"字

#### Scenario: 尺寸可配置
- **GIVEN** 尺寸参数为 32.dp
- **WHEN** 渲染 CategoryCircleIcon
- **THEN** 显示 32dp 圆形

#### Scenario: 默认值
- **WHEN** 不传 modifier 参数
- **THEN** 使用默认 `Modifier`

### Requirement: DayGroupHeader 组件
系统 SHALL 提供 `DayGroupHeader` Composable，显示分组日期标签和当天收支摘要。

#### Scenario: 日期显示
- **GIVEN** 日期为今天（与系统日期同一天）
- **WHEN** 渲染 header
- **THEN** 左侧显示 "M月d日 今天"
- **WHEN** 日期为昨天
- **THEN** 左侧显示 "M月d日 昨天"
- **WHEN** 其他日期
- **THEN** 左侧显示 "M月d日 星期X"

#### Scenario: 收支摘要
- **GIVEN** 当天支出总和为 ¥200.00，收入总和为 ¥100.00
- **WHEN** 渲染 header
- **THEN** 右侧显示 "出 ¥200.00 入 ¥100.00"

#### Scenario: 无收入时
- **GIVEN** 当天只有支出 ¥50.00，无收入
- **WHEN** 渲染 header
- **THEN** 右侧只显示 "出 ¥50.00"

### Requirement: 按天分组 LazyColumn
系统 SHALL 将 `filteredExpenses` 按日期分组，使用 `stickyHeader` 显示日期分组头部，组内记录按时间从新到旧排列。

#### Scenario: 分组结构
- **GIVEN** filteredExpenses 包含 2 天的记录
- **WHEN** 渲染 LazyColumn
- **THEN** 每天对应一个 stickyHeader + 多条记录 items

#### Scenario: 记录排序
- **GIVEN** 同一天有 3 条记录（12:00、09:00、15:00）
- **WHEN** 渲染组内记录
- **THEN** 按 15:00 → 12:00 → 09:00 排列（新到旧）

### Requirement: 分组 key 稳定性
系统 SHALL 为 stickyHeader 使用 `key = "header_$dateKey"`，为每条记录使用 `key = expense.id` 确保重组稳定性。

#### Scenario: 列表性能
- **WHEN** 数据更新但 key 不变
- **THEN** LazyColumn 仅重组变化的项

### Requirement: 记录行组件
系统 SHALL 每条记录使用 `ExpenseRecordItem` 组件，包含：分类图标圆（40dp） + 时间/商户信息 + 金额（支出红/收入绿）。

#### Scenario: 支出记录
- **GIVEN** 金额为正值（支出）
- **WHEN** 渲染记录
- **THEN** 金额显示 "-¥XXX.XX"，红色

#### Scenario: 收入记录
- **GIVEN** 金额为负值（收入）
- **WHEN** 渲染记录
- **THEN** 金额显示 "+¥XXX.XX"，绿色（tertiary）

#### Scenario: 无商户名
- **GIVEN** expense.merchant 为空
- **WHEN** 渲染记录
- **THEN** 子行只显示时间（HH:mm），不显示分隔符和商户名

#### Scenario: 有商户名
- **GIVEN** expense.merchant 为 "美团外卖"
- **WHEN** 渲染记录
- **THEN** 子行显示 "HH:mm  |  美团外卖"

### Requirement: 保留左滑删除
系统 SHALL 保留每条记录的 SwipeToDismissBox 左滑删除功能（EndToStart），滑动后弹出删除确认对话框。

#### Scenario: 左滑操作
- **GIVEN** 用户左滑某条记录
- **WHEN** 滑动到位
- **THEN** 弹出删除确认对话框

### Requirement: 保留筛选功能
系统 SHALL 保留 FilterBar（平台、分类、月份下拉筛选），筛选后实时更新分组列表。

#### Scenario: 筛选联动
- **GIVEN** 用户选择平台"微信"
- **WHEN** 筛选生效
- **THEN** 分组列表仅显示微信平台的记录

### Requirement: 保留手动添加和编辑
系统 SHALL 保留 AddExpenseSheet（手动添加 BottomSheet）和 EditRecordSheet（编辑 BottomSheet）。

#### Scenario: 添加新记录
- **GIVEN** 用户点击"手动添加"
- **WHEN** 添加完成
- **THEN** 列表自动刷新显示新记录

### Requirement: 记录间分隔线
系统 SHALL 在同组记录之间使用 0.5dp 的 HorizontalDivider，左侧从 68dp 处开始缩进，避免与分类图标圆重叠。

#### Scenario: 分隔线渲染
- **GIVEN** 同组有 2 条以上记录
- **WHEN** 渲染
- **THEN** 记录间有缩进分隔线