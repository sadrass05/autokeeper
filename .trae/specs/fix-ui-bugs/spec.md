# UI Bug 修复 Spec

## Why
列表项整行背景变红影响阅读；左滑删除垃圾桶图标静止时露出；理财多选操作栏右侧按钮被挤压导致无法点击。

## What Changes
- 移除列表项容器上的红色背景，仅保留金额文字的红色
- 修复 SwipeToDismiss 背景层/内容层层级关系
- 理财多选操作栏左侧文本加 weight，右侧按钮组 wrapContentWidth

## Impact
- Affected code: RecordsScreen.kt (SwipeableRecordItem), HomeScreen.kt (TransactionItem), FinanceScreen.kt (多选操作栏)

---

## ADDED Requirements

### Requirement: 列表项背景透明
交易记录列表项 SHALL 使用透明或无背景，不使用 Error 色。

#### Scenario: 列表项显示
- **WHEN** 用户查看交易记录
- **THEN** 列表项整行无红色背景
- **AND** 仅金额文字使用 error 色

### Requirement: 左滑删除图标隐藏
SwipeToDismiss 的删除图标 SHALL 在静止状态下被内容层完全遮挡。

#### Scenario: 静止状态
- **WHEN** 列表项未滑动
- **THEN** 垃圾桶图标不可见

### Requirement: 多选操作栏不压缩按钮
理财多选操作栏右侧按钮 SHALL 在选中大量项时保持可点击。

#### Scenario: 大量选中
- **WHEN** 用户全选超过 1000 条持仓
- **THEN** "已选 N 项"文本自适应宽度
- **AND** 右侧取消/全选/删除按钮不被压缩，文字完整可见
