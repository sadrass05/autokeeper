# 记录显示、删除确认与回收站 Spec

## Why
交易记录滑动删除无确认易误删且不可恢复；记录列表和首页缺少日期时间展示；理财多选缺少全选功能。

## What Changes
- 滑动删除改为弹确认对话框，软删除（isDeleted 标记）替代真删
- ExpenseRecord 新增 isDeleted 字段，Room 数据库升级迁移
- 设置页新增回收站，可恢复/清空已删记录，30 天自动清理
- 所有记录条目副标题增加日期时间显示"MM-dd HH:mm"
- 理财多选操作栏增加全选/取消全选按钮

## Impact
- Affected specs: fix-export-finance-records
- Affected code: ExpenseRecord.kt, ExpenseDao.kt, AppDatabase.kt, RecordsScreen.kt, HomeScreen.kt, FinanceScreen.kt, SettingsScreen.kt, MainViewModel.kt
- New files: TrashScreen.kt

---

## ADDED Requirements

### Requirement: 删除确认对话框
记录滑动删除 SHALL 弹出确认对话框，用户确认后才执行删除。

#### Scenario: 滑动删除确认
- **WHEN** 用户滑动某条记录到删除位置
- **THEN** 弹出 AlertDialog："确定要删除这条记录吗？"
- **AND** 点击"取消"不执行删除，记录恢复原位
- **AND** 点击"确定"执行软删除

### Requirement: 软删除机制
删除操作 SHALL 将记录的 isDeleted 设为 true，不物理删除数据。

#### Scenario: 软删除执行
- **WHEN** 用户确认删除某记录
- **THEN** 该记录 isDeleted 设为 true
- **AND** 所有列表查询自动过滤 isDeleted=true 的记录
- **AND** 统计计算（今日支出、本月支出等）排除已删除记录

### Requirement: 回收站
设置页 SHALL 提供回收站入口，可查看、恢复已删记录。

#### Scenario: 查看回收站
- **WHEN** 用户点击设置页"回收站"
- **THEN** 进入回收站页面，列出所有 isDeleted=true 的记录
- **AND** 每条显示分类、金额、平台、删除日期

#### Scenario: 恢复记录
- **WHEN** 用户在回收站点击某条记录的"恢复"按钮
- **THEN** 该记录 isDeleted 设为 false
- **AND** 记录重新出现在交易列表中
- **AND** 统计数据自动更新

#### Scenario: 清空回收站
- **WHEN** 用户点击"清空回收站"
- **THEN** 弹出确认对话框
- **AND** 确认后物理删除所有 isDeleted=true 的记录

#### Scenario: 30 天自动清理
- **WHEN** 应用启动时
- **THEN** 系统检查 isDeleted=true 且删除超过 30 天的记录
- **AND** 自动物理删除这些过期记录

### Requirement: 记录日期时间显示
支出记录条目 SHALL 在副标题行显示交易日期时间。

#### Scenario: 记录列表显示日期
- **WHEN** 用户查看交易记录列表
- **THEN** 副标题显示"平台 · 支付渠道  |  MM-dd HH:mm"格式

#### Scenario: 首页最近交易显示日期
- **WHEN** 用户在首页查看最近交易
- **THEN** 副标题同样显示日期时间

### Requirement: 理财多选全选
理财多选模式 SHALL 提供全选/取消全选功能。

#### Scenario: 全选操作
- **WHEN** 用户在理财页多选模式下点击"全选"
- **THEN** 当前所有持仓条目被勾选
- **AND** 按钮文案变为"取消全选"

#### Scenario: 取消全选
- **WHEN** 已全部选中时点击"取消全选"
- **THEN** 取消所有勾选

## MODIFIED Requirements

### Requirement: 删除操作语义（修改）
**原行为**：滑动删除立即物理删除记录，无确认。
**修改后**：滑动后弹确认对话框，确认后软删除（isDeleted=true）。

### Requirement: ExpenseRecord 实体（修改）
**原行为**：无 isDeleted 字段。
**修改后**：新增 isDeleted: Boolean = false，数据库 V2 迁移。
