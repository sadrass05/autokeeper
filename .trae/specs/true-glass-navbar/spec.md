# 真实毛玻璃导航栏 Spec

## Why
当前导航栏使用简单的 `background(alpha) + blur` + 单一边框，缺乏真毛玻璃质感。底层内容透过导航栏无法产生明显的模糊朦胧感，视觉上仍是"半透明色块"而非"毛玻璃"。

## What Changes
- GlassNavigationBar 容器从单层 Box 改为 4 层叠加结构（模糊扩散层 + 磨砂渐变层 + 高光描边层 + 内容层）
- 浅色模式三重叠加：surface 模糊层 + White 渐变磨砂 + White 高光描边
- 深色模式适配：`#1C1C1E` 模糊层 + `#2C2C2E@0.8` 磨砂 + `White@0.12/0.04` 高光
- 阴影从 16dp + ambientColor 升级为 20dp + spotColor + ambientColor
- 使用 `matchParentSize()` 自动匹配父容器尺寸
- 使用 `Brush.verticalGradient` 实现光照渐变
- MainActivity Scaffold 添加 `contentWindowInsets = WindowInsets(0)`，让页面内容延伸至导航栏下方
- **BREAKING**: GlassNavigationBar 容器从单层 Box 改为多层 overlay 结构

## Impact
- Affected specs: liquid-glass-navbar, fix-navbar-icons
- Affected code: GlassNavigationBar.kt（GlassNavigationBar 函数）MainActivity.kt（contentWindowInsets）
- New files: 无

---

## ADDED Requirements

### Requirement: 模糊扩散层（第1层）
系统 SHALL 在导航栏底部渲染一层模糊扩散背景。

#### Scenario: 浅色模糊层
- **WHEN** 浅色主题激活
- **THEN** Box(matchParentSize) + background = MaterialTheme.colorScheme.surface
- **AND** blur(radius = 30.dp, edgeTreatment = BlurEdgeTreatment.Unbounded)

#### Scenario: 深色模糊层
- **WHEN** 深色主题激活
- **THEN** Box(matchParentSize) + background = Color(0xFF1C1C1E)
- **AND** blur(radius = 30.dp, edgeTreatment = BlurEdgeTreatment.Unbounded)

### Requirement: 半透明磨砂渐变层（第2层）
系统 SHALL 在模糊层之上叠加渐变半透明遮罩。

#### Scenario: 浅色磨砂
- **WHEN** 浅色主题激活
- **THEN** Box(matchParentSize) + Brush.verticalGradient(White@0.75 → White@0.60)

#### Scenario: 深色磨砂
- **WHEN** 深色主题激活
- **THEN** Box(matchParentSize) + Color(0xFF2C2C2E).copy(alpha = 0.80f)

### Requirement: 高光描边层（第3层）
系统 SHALL 在磨砂层之上添加渐变描边模拟玻璃边缘高光。

#### Scenario: 浅色高光
- **WHEN** 浅色主题激活
- **THEN** Box(matchParentSize) + border(1.dp, Brush.verticalGradient(White@0.8 → White@0.1), RoundedCornerShape(32.dp))

#### Scenario: 深色高光
- **WHEN** 深色主题激活
- **THEN** Box(matchParentSize) + border(1.dp, Brush.verticalGradient(White@0.12 → White@0.04), RoundedCornerShape(32.dp))

### Requirement: 内容层（第4层）
系统 SHALL 在最上层渲染导航项 Row。

#### Scenario: 内容渲染
- **WHEN** 导航栏渲染
- **THEN** Row(matchParentSize, padding(horizontal=6dp, vertical=4dp)) 包含 GlassNavItem 列表

### Requirement: 增强阴影
系统 SHALL 使用 20dp 阴影 + spotColor 提供更强悬浮感。

#### Scenario: 阴影效果
- **WHEN** 导航栏渲染
- **THEN** shadow(elevation=20.dp, RoundedCornerShape(32.dp), spotColor=Black@0.12, ambientColor=Black@0.06)

### Requirement: 页面内容延伸
系统 SHALL 设置 Scaffold contentWindowInsets = WindowInsets(0) 让内容延伸至导航栏下方。

#### Scenario: 内容透过
- **WHEN** 页面渲染
- **THEN** Scaffold 内容可延伸至屏幕底部
- **AND** GlassNavigationBar 作为 overlay 悬浮于内容之上
- **AND** 毛玻璃效果透出底层模糊内容

### Requirement: 深浅色自适应
系统 SHALL 根据 MaterialTheme.colorScheme.background 判断深浅色模式。

#### Scenario: 模式切换
- **WHEN** background == DarkBackground（即 Color(0xFF121212)）
- **THEN** 使用深色模式配色
- **WHEN** background != DarkBackground
- **THEN** 使用浅色模式配色

## MODIFIED Requirements

### Requirement: GlassNavigationBar 容器结构
系统 SHALL 使用 clip(RoundedCornerShape(32.dp)) 包裹整个导航栏，并在 clip 内使用 4 层叠加 Box。

#### Scenario: 容器渲染
- **WHEN** 渲染 GlassNavigationBar
- **THEN** 外层 padding(horizontal=16dp) + fillMaxWidth + height(64.dp)
- **AND** shadow(20.dp, ...) + clip(RoundedCornerShape(32.dp))
- **AND** 内部 4 层叠加 Box + Row

## REMOVED Requirements

### Requirement: 单层 background + blur 实现
**Reason**: 被 4 层叠加方案替代。
**Migration**: 旧代码中 `.background(glassBg, shape).blur(20.dp).border(BorderStroke(...))` 全部移除，改为 4 层 Box 结构。