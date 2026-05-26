# 毛玻璃导航栏色彩精调 Spec

## Why
当前透明度配色导致毛玻璃缺乏厚实遮盖感和镜面反射高光，尤其在深色模式下视觉上仍是"半透明色块"。需要增强模糊层遮盖力、磨砂层实底感和描边的镜面高光对比度。

## What Changes
- GlassNavigationBar.kt 仅调整 6 个颜色值
- MainActivity.kt 不变
- 4 层 DOM 结构不变

## Impact
- Affected code: GlassNavigationBar.kt（仅 blurBg / frostBrush / edgeBrush 颜色值）
- New files: 无

---

## MODIFIED Requirements

### Requirement: 模糊层遮盖力增强
系统 SHALL 提高模糊层 alpha 值以产生更厚实的遮盖感。

#### Scenario: 浅色
- **WHEN** 浅色主题激活
- **THEN** blurBg = Color(0xFFE8E8E8).copy(alpha = 0.75f)

#### Scenario: 深色
- **WHEN** 深色主题激活
- **THEN** blurBg = Color(0xFF1C1C1E).copy(alpha = 0.88f)

### Requirement: 磨砂层纯度提升
系统 SHALL 调整磨砂层为更纯净的半透明遮罩。

#### Scenario: 浅色
- **WHEN** 浅色主题激活
- **THEN** frostBrush = Color.White.copy(alpha = 0.65f) 单色

#### Scenario: 深色
- **WHEN** 深色主题激活
- **THEN** frostBrush = Color(0xFF2C2C2E).copy(alpha = 0.75f)

### Requirement: 镜面高光增强
系统 SHALL 提高描边渐变的亮度对比。

#### Scenario: 浅色
- **WHEN** 浅色主题激活
- **THEN** edgeBrush = White@0.90 → White@0.20

#### Scenario: 深色
- **WHEN** 深色主题激活
- **THEN** edgeBrush = White@0.15 → White@0.03