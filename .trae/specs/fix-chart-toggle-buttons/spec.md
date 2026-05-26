# 图表切换按钮不可见修复 Spec

## Why
HomeScreen 支出趋势图表折线/柱状切换按钮功能正常但完全不可见。根因是使用 `R.drawable.ic_home`（房屋图标）和 `R.drawable.ic_records`（列表图标）作为图表切换图标——这些矢量 drawable 非图表语义，且在该主题下 tint 无法正常渲染覆盖原有颜色定义。

## What Changes
- 图标按钮（IconButton + vectorResource）替换为 FilledTonalButton + Text("折线"/"柱状")
- 选中态：containerColor = primary.copy(alpha=0.15f), contentColor = primary
- 未选中态：containerColor = Color.Transparent, contentColor = onSurfaceVariant
- 按钮样式：height=32dp, contentPadding(horizontal=12dp, vertical=4dp), 间距 6dp

## Impact
- Affected code: HomeScreen.kt（图表切换按钮区域，L164-L183）
- New files: 无

---

## MODIFIED Requirements

### Requirement: 图表切换按钮样式
系统 SHALL 使用文字标签按钮替代图标按钮，确保在深浅色主题下始终可见。

#### Scenario: 选中态
- **WHEN** 按钮对应的图表类型被选中
- **THEN** containerColor = primary.copy(alpha = 0.15f)
- **AND** contentColor = primary

#### Scenario: 未选中态
- **WHEN** 按钮对应的图表类型未选中
- **THEN** containerColor = Color.Transparent
- **AND** contentColor = onSurfaceVariant