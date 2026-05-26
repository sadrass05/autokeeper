# 修复点击支出记录闪退 Spec

## Why
点击交易记录页面的支出记录时应用闪退。根本原因是 RecordsScreen.kt 中 SwipeableRecordItem 的时间格式化代码使用了无效的 API：`LocalLocale.current.platformLocale`。在 Compose BOM 2024.08.00 中，`LocalLocale.current` 返回 `java.util.Locale`，该类没有 `platformLocale` 属性，导致运行时 `NoSuchFieldError` 崩溃。

## What Changes
- **修改** `RecordsScreen.kt`：将 `LocalLocale.current.platformLocale` 替换为 `Locale.getDefault()`
- 移除未使用的 `LocalLocale` 导入

## Impact
- Affected specs: `day-grouped-records`（该行代码是在该 spec 重构时引入的）
- Affected code: `ui/screen/RecordsScreen.kt`

## MODIFIED Requirements

### Requirement: 交易记录时间格式化
系统 SHALL 使用 `Locale.getDefault()` 格式化记录时间，而非无效的 `LocalLocale.current.platformLocale`。

#### Scenario: 点击记录查看详情
- **GIVEN** 用户查看交易记录列表，其中一条记录的 `recordedAt` 为某时间戳
- **WHEN** 用户点击该记录
- **THEN** 应用不崩溃，EditRecordSheet 正常弹出

#### Scenario: 记录时间显示
- **GIVEN** 一条记录的 `recordedAt` 时间戳
- **WHEN** 渲染该记录行
- **THEN** 子行正确显示 "HH:mm" 格式的时间文本