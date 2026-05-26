# 合并支出概览卡片 Spec

## Why
HomeScreen 当前使用两个独立 GlassCard 分别显示"今日支出"和"本月支出"，占用了过多垂直空间且视觉分散。需要将它们合并为一个卡片，今日支出作为主信息大字显示，本月支出作为次要信息小字显示，形成更有层次感的支出概览区域。

## What Changes
- **修改** `HomeScreen.kt`：删除两个独立 GlassCard，替换为单个 Card 合并卡片
- 移除 `verticalArrangement = Arrangement.spacedBy(16.dp)` 的 Column 容器

## Impact
- Affected specs: 无
- Affected code: `ui/screen/HomeScreen.kt`

## MODIFIED Requirements

### Requirement: 支出概览卡片
系统 SHALL 使用单个 Card 组件同时展示今日支出和本月支出，今日支出为大字主信息，本月支出为小字次要信息，整体居中对齐。

#### Scenario: 今日支出主信息
- **GIVEN** 当日支出为 ¥50.00
- **WHEN** 渲染支出概览卡片
- **THEN** 标签显示"今日支出"，金额以 `displaySmall` + `FontWeight.Bold` + `error` 颜色显示 "¥50.00"

#### Scenario: 本月支出次要信息
- **GIVEN** 当月支出为 ¥1234.56
- **WHEN** 渲染支出概览卡片
- **THEN** 分割线下方以 `bodySmall` 标签 + `titleMedium` 金额显示 "本月支出  ¥1234.56"

#### Scenario: 分割线分隔
- **WHEN** 渲染支出概览卡片
- **THEN** 今日支出与本月支出之间有一条 0.5dp 的 HorizontalDivider，宽度为卡片 60%

## REMOVED Requirements

### Requirement: 独立今日支出卡片
**Reason**: 合并为单个卡片，不再需要独立的 GlassCard
**Migration**: 代码已内联在合并卡片中，无数据迁移

### Requirement: 独立本月支出卡片
**Reason**: 合并为单个卡片，不再需要独立的 GlassCard
**Migration**: 代码已内联在合并卡片中，无数据迁移
