# 理财收益支出记录 + 持仓收益排行榜 Spec

## Why
当前理财板块（FinanceScreen）仅有持仓明细列表，缺少两个核心功能：收益支出记录（追踪从理财账户划出的消费）和持仓收益排行榜（按收益排序展示），用户无法完整管理理财资金流和对比各产品收益表现。

## What Changes
### 功能一：理财收益支出记录
- **新增** `data/entity/FinanceExpense.kt` — 理财支出实体（id, amount, description, fromProduct, recordedAt）
- **新增** `data/dao/FinanceExpenseDao.kt` — 支出记录 DAO（增删查+总额查询）
- **新增** `data/repository/FinanceExpenseRepository.kt` — 支出记录 Repository
- **修改** `data/AppDatabase.kt` — 版本升至 4，新增 `finance_expenses` 表 + MIGRATION_3_4
- **修改** `di/DatabaseModule.kt` — 提供 `FinanceExpenseDao` 和 `FinanceExpenseRepository`
- **修改** `ui/viewmodel/MainViewModel.kt` — 新增 `financeExpenses` StateFlow + `addFinanceExpense`/`deleteFinanceExpense` 方法，`calculateFinanceExpense` 改为从新表汇总
- **修改** `ui/screen/FinanceScreen.kt` — 新增收益支出记录区块（含 BottomSheet 表单）+ 更新概览卡片

### 功能二：持仓收益排行榜
- **修改** `ui/screen/FinanceScreen.kt` — 新增收益排行区块（含排序切换 + RankBadge + animateItemPlacement）

### 功能三：HomeScreen 按钮清理
- **修改** `ui/screen/HomeScreen.kt` — 删除底部"添加理财"和"导出报表"两个按钮及相关参数
- **修改** `MainActivity.kt` — 移除 `onExportReport` 调用（如有）

## Impact
- Affected specs: `enhance-home-finance-ui`（理财 UI 增强）
- Affected code:
  - `data/entity/FinanceExpense.kt`（新建）
  - `data/dao/FinanceExpenseDao.kt`（新建）
  - `data/repository/FinanceExpenseRepository.kt`（新建）
  - `data/AppDatabase.kt`
  - `di/DatabaseModule.kt`
  - `ui/viewmodel/MainViewModel.kt`
  - `ui/screen/FinanceScreen.kt`
  - `ui/screen/HomeScreen.kt`
  - `MainActivity.kt`

## ADDED Requirements

### Requirement: 理财收益支出记录
系统 SHALL 提供独立的理财收益支出记录功能，用户可记录从理财产品中划出的消费。

#### Scenario: 查看收益支出记录
- **WHEN** 用户进入 FinanceScreen
- **THEN** 持仓明细上方显示"收益支出记录"区块，展示最近5条记录（用途描述+来源产品+金额+日期）

#### Scenario: 添加收益支出记录
- **WHEN** 用户点击"+ 记录支出"按钮
- **THEN** 弹出 ModalBottomSheet 表单（金额、用途描述、来源产品下拉、日期选择、确认按钮）

#### Scenario: 概览卡片同步
- **WHEN** 用户添加/删除理财支出记录
- **THEN** 顶部资产概览卡片的"理财收益支出"和"剩余净收益"自动更新

### Requirement: 持仓收益排行榜
系统 SHALL 提供按收益排序的持仓排行榜，支持按收益率/收益额切换排序。

#### Scenario: 排行展示
- **WHEN** 用户进入 FinanceScreen
- **THEN** 在收益支出记录下方显示"收益排行"区块，各产品按收益降序排列，1-3名显示特殊金/银/铜徽章

#### Scenario: 排序切换
- **WHEN** 用户点击"按收益率排序"或"按收益额排序"按钮
- **THEN** 列表以 animateItemPlacement 动画重新排序

### Requirement: HomeScreen 底部按钮清理
系统 SHALL 从 HomeScreen 底部移除"添加理财"和"导出报表"两个按钮及相关参数。

#### Scenario: 按钮已移除
- **WHEN** 用户查看 HomeScreen 底部
- **THEN** 不再显示"添加理财"和"导出报表"按钮，布局不留空白

## MODIFIED Requirements

### Requirement: 理财收益支出数据源
理财收益支出的计算从 `expenses` 表的 `isFinanceExpense=true` 记录改为 `finance_expenses` 表的 `SUM(amount)`。

**Migration**: 已有 `isFinanceExpense` 字段的支出记录保持不变（属于旧版支出分类），新版理财支出通过 FinanceExpense 表独立管理。