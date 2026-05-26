# 底部导航栏图标显示修复 Spec

## Why
导航栏图标颜色使用 `onSurfaceVariant` 导致未选中态视觉过亮，且 Column 缺少 `fillMaxHeight()` 可能导致图标被压缩。同时缺少选中指示器，用户无法直观判断当前页面。

## What Changes
- GlassNavItem 未选中图标 tint 从 `onSurfaceVariant` 改为 `onSurface.copy(alpha=0.6f)`
- Column 新增 `.fillMaxHeight()` 防止高度塌陷
- padding(vertical) 从 6.dp 调整为 8.dp
- 新增 2dp 高、24dp 宽的 primary 色圆角矩形选中指示器，使用 AnimatedVisibility
- GlassNavigationBar 容器 `height(64.dp)` 保持不变

## Impact
- Affected specs: liquid-glass-navbar
- Affected code: GlassNavigationBar.kt（仅 GlassNavItem 函数）
- New files: 无

---

## MODIFIED Requirements

### Requirement: 导航项图标颜色
系统 SHALL 未选中图标使用 onSurface.copy(alpha=0.6f) 作为 tint 色。

#### Scenario: 图标颜色
- **WHEN** 导航项未选中
- **THEN** 图标 tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
- **AND** 选中图标 tint = MaterialTheme.colorScheme.primary

### Requirement: 导航项容器布局
系统 SHALL 使用 Column + fillMaxHeight() 防止高度塌陷。

#### Scenario: 容器约束
- **WHEN** 渲染导航项
- **THEN** Column 使用 Modifier.weight(1f).fillMaxHeight()
- **AND** padding(vertical = 8.dp)

### Requirement: 选中指示器
系统 SHALL 在图标下方显示 2dp 高 primary 色圆角矩形指示条。

#### Scenario: 指示器显隐
- **WHEN** 导航项选中
- **THEN** 显示 24dp × 2dp 圆角矩形色条（RoundedCornerShape(1.dp), primary 色）
- **AND** 使用 AnimatedVisibility 控制显隐动画
- **WHEN** 导航项未选中
- **THEN** 指示条隐藏