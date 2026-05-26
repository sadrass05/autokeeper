# 底部毛玻璃导航栏透明度修复 Spec

## Why
毛玻璃导航栏效果消失，视觉上退化为不透明色块。根因是第1层模糊层的背景使用了完全不透明的 `surface`（#FFFFFF）或 `#1C1C1E`，`Modifier.blur()` 在 Compose 中模糊的是组件自身绘制内容，无法透视底层。需将模糊层改为半透明色，让磨砂质感和羽化边缘透过 `blur` 扩散产生。

## What Changes
- GlassNavigationBar 第1层模糊扩散层：background 从 `blurBg`（不透明）改为 `frostBgColor.copy(alpha = 0.5f)`（半透明）
- 深浅色模式下 blur 背景分别取对应的半透明色
- 保持其余 3 层和 MainActivity overlay 结构不变

## Impact
- Affected specs: true-glass-navbar
- Affected code: GlassNavigationBar.kt（仅第1层 Box modifier）
- New files: 无

---

## MODIFIED Requirements

### Requirement: 模糊扩散层背景透明度
系统 SHALL 将第1层模糊扩散层的背景改为半透明，使 blur 产生柔和羽化边缘而非不透明色块。

#### Scenario: 浅色模糊层
- **WHEN** 浅色主题激活
- **THEN** 第1层 Box background = Color.White.copy(alpha = 0.5f)
- **AND** blur(radius = 30.dp) 在半透明白色上产生羽化扩散

#### Scenario: 深色模糊层
- **WHEN** 深色主题激活
- **THEN** 第1层 Box background = Color(0xFF1C1C1E).copy(alpha = 0.5f)
- **AND** blur(radius = 30.dp) 在半透明深色上产生羽化扩散