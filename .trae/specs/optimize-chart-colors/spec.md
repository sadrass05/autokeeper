# 图表颜色方案优化 Spec

## Why
当前首页图表的颜色方案较为单调：折线图和柱状图都使用主题色 primary，MonthlyBarChart 仅用 primary/dim 二色区分当前月和历史月。需要更丰富生动的配色来提升视觉吸引力。

## What Changes
- **修改** `TrendChart.kt` — 折线图颜色改为青绿色(0xFF26A69A)，线宽2.5f，圆点4f；柱状图模式改为多色暖色调
- **修改** `MonthlyBarChart.kt` — 从 primary/dim 二色改为 7 色暖色调渐变色系

## Impact
- Affected specs: 无
- Affected code:
  - `ui/components/TrendChart.kt`
  - `ui/components/MonthlyBarChart.kt`

## MODIFIED Requirements

### Requirement: 柱状图多色方案
系统 SHALL 为柱状图的每根柱子分配不同颜色，使用暖色调渐变色系（7色），MPAndroidChart 自动循环使用。

#### Scenario: MonthlyBarChart 多色
- **GIVEN** 柱状图有 6 根柱子
- **WHEN** 渲染 MonthlyBarChart
- **THEN** 每根柱子依次使用暖色调色板中的不同颜色

#### Scenario: TrendChart BAR 模式多色
- **GIVEN** TrendChart 在柱状图模式下渲染
- **WHEN** 渲染
- **THEN** 每根柱子使用暖色调多色，不再使用单一 primaryColor

#### Scenario: 颜色饱和度适中
- **WHEN** 任意柱状图渲染
- **THEN** 颜色饱和度适中，不刺眼，在浅色/深色背景下均可辨识

### Requirement: 折线图颜色替换
系统 SHALL 将折线图线条颜色改为青绿色 0xFF26A69A，填充色为线条色的 20% 透明度。

#### Scenario: 折线颜色
- **GIVEN** TrendChart 在折线图模式下渲染
- **WHEN** 渲染
- **THEN** 线条颜色为 0xFF26A69A，线宽 2.5f

#### Scenario: 数据点圆圈
- **GIVEN** TrendChart 在折线图模式下渲染
- **WHEN** 渲染
- **THEN** 数据点显示 4f 半径圆圈，颜色与线条一致

#### Scenario: 填充色
- **GIVEN** TrendChart 在折线图模式下渲染
- **WHEN** 渲染
- **THEN** 填充色为青绿色 20% 透明度

## REMOVED Requirements

### Requirement: 主题色折线
**Reason**: 替换为更鲜明的青绿色
**Migration**: 无，纯视觉变更

### Requirement: 主题色/暗色二值柱状图
**Reason**: MonthlyBarChart 的 primary/dim 二色方案替换为 7 色暖色调
**Migration**: 无，纯视觉变更
