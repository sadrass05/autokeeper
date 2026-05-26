# 本月支出排行榜 Spec

## Why
HomeScreen 当前展示每日/每月汇总、支出趋势和月度对比图表，但缺少本月高额支出条目的明细展示。用户需要快速看到本月花钱最多的5笔记录。

## What Changes
- 新增 `TopExpensesCard.kt` 组件（ui/components/）
- 在 `MainViewModel` 中新增 `topExpenses: StateFlow<List<ExpenseRecord>>`（从 expenses flow 派生）
- 在 `HomeScreen` 函数签名中新增 `onNavigateToRecords: () -> Unit` 回调
- 在 `MainActivity` 中传递 `onNavigateToRecords = { selectedScreen = 1 }`
- 在 `HomeScreen.kt` 中插入排行榜卡片（月度对比图表下方、当日支出分类上方）

## Impact
- Affected specs: 无（新功能）
- Affected code:
  - **新增**: `ui/components/TopExpensesCard.kt`
  - **修改**: `ui/viewmodel/MainViewModel.kt`（新增 topExpenses flow）
  - **修改**: `ui/screen/HomeScreen.kt`（新增回调参数 + 插入卡片）
  - **修改**: `MainActivity.kt`（传递新回调）

## ADDED Requirements

### Requirement: Top Expenses StateFlow
系统 SHALL 从 `expenses` StateFlow 派生出 `topExpenses: StateFlow<List<ExpenseRecord>>`，过滤本月记录、按金额降序排列、取前5条。

#### Scenario: 派生 Flow
- **GIVEN** expenses 包含本月多条记录
- **WHEN** topExpenses 被收集
- **THEN** 返回本月金额最大的5条记录，按降序排列
- **AND** 使用 `stateIn(SharingStarted.Lazily)` 实现

### Requirement: 排行榜卡片组件
系统 SHALL 提供 `TopExpensesCard` Composable 组件，接收 `topExpenses: List<ExpenseRecord>` 和 `onViewAll: () -> Unit`。

#### Scenario: 空数据
- **WHEN** topExpenses 为空
- **THEN** 不渲染任何内容（或显示空状态提示）

#### Scenario: 有数据
- **WHEN** topExpenses 非空
- **THEN** 渲染标题"本月支出排行"的 Card 容器，内含排行榜列表和底部"查看全部记录"按钮

### Requirement: 排名序号样式（三级）
系统 SHALL 根据排名位置使用不同样式：
- 第1名：主题色圆形背景（28dp）+ 白色数字（12sp, Medium）
- 第2、3名：浅色圆形背景（onSurface.copy(alpha=0.1f)）+ 深色数字（onSurface, 12sp）
- 第4、5名：纯文字数字（无背景，onSurfaceVariant, 12sp）

#### Scenario: 排名可视化
- **GIVEN** 5条记录
- **WHEN** 渲染排行榜
- **THEN** 第1名金色圆圈、第2-3名浅灰圆圈、第4-5名纯数字

### Requirement: 记录行布局
系统 SHALL 每条记录使用 Row 布局，包含：
- [序号] (28dp宽) → [分类图标圆] (40dp) → [中间：商户名+分类] → [右侧：金额+日期]
- 商户名字体 14sp Medium，分类 11sp onSurfaceVariant
- 金额 15sp SemiBold error色，日期 11sp onSurfaceVariant
- 行间距 vertical=10dp

#### Scenario: 记录展示
- **GIVEN** 排名第1的记录：商户"美团外卖"、分类"餐饮"、金额¥128.50
- **WHEN** 渲染排行榜
- **THEN** 行显示：① 金色序号 + 圆图标 + "美团外卖 / 餐饮" + "¥128.50 / 05-22 12:30"

### Requirement: 分类图标圆
系统 SHALL 为每条记录渲染 40dp 圆形图标，背景色根据分类名称映射颜色，中央显示分类首字符。

#### Scenario: 分类图标
- **GIVEN** 分类为"餐饮"
- **WHEN** 渲染图标
- **THEN** 显示橙色圆形背景 + 白色"餐"字（或餐饮对应图标）

### Requirement: 日期格式化
系统 SHALL 将 `recordedAt` 时间戳格式化为 "MM-dd HH:mm" 显示。

#### Scenario: 日期显示
- **GIVEN** recordedAt = 2026年5月22日 12:30 的时间戳
- **WHEN** 格式化
- **THEN** 显示 "05-22 12:30"

### Requirement: 查看全部记录按钮
系统 SHALL 在卡片底部提供 "查看全部记录 →" 文字按钮，点击触发 `onViewAll` 回调，切换到 Records 页面。

#### Scenario: 导航到记录页
- **GIVEN** 用户点击"查看全部记录"
- **WHEN** onViewAll 回调触发
- **THEN** MainActivity 切换 selectedScreen 到 index 1（Records）

### Requirement: HomeScreen 回调扩展
系统 SHALL 在 HomeScreen 函数签名中新增 `onNavigateToRecords: () -> Unit = {}` 参数，由 MainActivity 传入 `{ selectedScreen = 1 }`。

#### Scenario: 参数传递
- **GIVEN** HomeScreen 声明了 onNavigateToRecords
- **WHEN** MainActivity 构建 HomeScreen
- **THEN** 传入 `onNavigateToRecords = { selectedScreen = 1 }`

### Requirement: 页面插入位置
系统 SHALL 将排行榜卡片插入 HomeScreen LazyColumn 中月度对比图表之后、当日支出分类之前。

#### Scenario: 页面顺序
- **GIVEN** HomeScreen 渲染
- **WHEN** 用户滚动
- **THEN** 依次看到：月度支出对比 → **本月支出排行** → 当日支出分类 → 最近交易