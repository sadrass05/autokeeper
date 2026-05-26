# 图表闪烁与性能优化 Spec

## Why
支出趋势图表每次页面滚动时都被重建和刷新，因为 AndroidView 的 update lambda 无条件创建新 DataSet 并 invalidate，LazyColumn item 无稳定 key 也可能导致销毁重建。

## What Changes
- TrendChart.kt：update lambda 添加数据相等性检查，数据相同时跳过 invalidate
- TrendChart.kt：颜色 Argb 值用 remember 缓存，避免每次重组重建
- HomeScreen.kt：图表 item 添加稳定 key `"expense_chart"` 防止 LazyColumn 缓存回收

## Impact
- Affected code: TrendChart.kt, HomeScreen.kt
- New files: 无

---

## MODIFIED Requirements

### Requirement: AndroidView update 数据相同时跳过重绘
系统 SHALL 在图表 update lambda 中比较新旧数据，相同时不调用 invalidate。

#### Scenario: 数据未变化
- **WHEN** 页面重组
- **THEN** 如果 chart.data 与 newData 相等（相同条目数 + 相同值）
- **AND** chart.invalidate() 不被调用
- **AND** 图表不闪烁

### Requirement: LazyColumn item 稳定 key
系统 SHALL 为图表 item 使用固定字符串 key 防止不必要的销毁重建。

#### Scenario: 页面滚动
- **WHEN** LazyColumn 滚动
- **THEN** `item(key = "expense_chart")` 确保图表不被释放重建
- **AND** 图表保持渲染状态不变