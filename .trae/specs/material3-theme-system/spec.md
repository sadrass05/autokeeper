# Material3 主题系统重建 Spec

## Why
当前主题系统使用两套色调（AmberGold `#F59E0B` + DeepBlue `#2563EB`）、Color.kt 堆积 40+ 常量、字体层级过于冗长（13级全量覆盖）、缺少 Shape 系统。需要建立专业统一的记账 App Material3 主题体系。

## What Changes
- Color.kt 精简：移除 40+ 旧常量，只保留主题直接引用的 10 个颜色
- primary 从亮琥珀 `#F59E0B` 改为琥珀金铜 `#B87333`
- background 浅色从冷灰白 `#F5F5F7` 改为暖灰白 `#F5F5F0`
- background 深色从极深蓝黑 `#0B0E14` 改为标准 `#121212`
- surface/surfaceVariant 深浅色统一调整
- tertiary 从亮绿 `#22C55E` 改为沉稳收益绿 `#43A047`
- Type.kt 从 13 级全量覆盖精简为 4 核心级 + 其余默认
- 新建 Shape.kt（4 级圆角：8/12/16/24dp）
- Theme.kt 重写 DarkColorScheme 和 LightColorScheme，添加 shapes 参数
- **BREAKING**: 删除 DeepBlue 系列、旧 AmberGold 变体、PositiveGreen/NegativeRed 等旧常量

## Impact
- Affected specs: ui-overhaul, liquid-glass-navbar
- Affected code: Color.kt, Theme.kt, Type.kt（重写）, Shape.kt（新建）
- 下游影响：所有直接引用旧颜色常量（AmberGoldDark, AmberGoldLight, AmberGoldContainer, DeepBlue, PositiveGreen, NegativeRed, WarmBackground, DarkBackgroundDeep 等）的代码需迁移到 MaterialTheme.colorScheme API

---

## ADDED Requirements

### Requirement: Shape 圆角系统
系统 SHALL 提供统一的 4 级 Shape 规范。

#### Scenario: Shape 层级
- **WHEN** 组件需要圆角
- **THEN** small=8dp, medium=12dp, large=16dp, extraLarge=24dp
- **AND** extraSmall 保持 MD3 默认 4dp

### Requirement: 收益绿 tertiary
系统 SHALL 使用 Color(0xFF43A047) 作为 tertiary（收益/正向语义色）。

#### Scenario: 收益显示
- **WHEN** 界面需要显示收益/正向金额
- **THEN** 使用 tertiary 颜色渲染
- **AND** onTertiary 为 Color.White

## MODIFIED Requirements

### Requirement: 主色调（替换旧 AmberGold）
系统 SHALL 使用琥珀金铜 Color(0xFFB87333) 作为 primary。

#### Scenario: 主色渲染
- **WHEN** 任何组件使用 primary 色
- **THEN** 渲染为 `#B87333` 铜色调
- **AND** onPrimary 为 Color.White

### Requirement: 背景色（浅色模式）
系统 SHALL 浅色模式使用 Color(0xFFF5F5F0) 作为 background，更温暖的米白基调。

#### Scenario: 浅色背景
- **WHEN** 浅色主题激活
- **THEN** background = `#F5F5F0`（柔和暖灰白）
- **AND** surface = `#FFFFFF`（纯白）
- **AND** surfaceVariant = `#F0EDE8`（暖灰变体）

### Requirement: 背景色（深色模式）
系统 SHALL 深色模式使用 Color(0xFF121212) 作为 background，Color(0xFF1E1E1E) 作为 surface。

#### Scenario: 深色背景
- **WHEN** 深色主题激活
- **THEN** background = `#121212`（标准 Material 深色）
- **AND** surface = `#1E1E1E`
- **AND** surfaceVariant = `#2C2C2C`

### Requirement: 错误色
系统 SHALL 使用 Color(0xFFE53935) 作为 error。

#### Scenario: 错误提示
- **WHEN** 显示错误状态
- **THEN** error = `#E53935`
- **AND** onError = Color.White

### Requirement: 字体层级（精简）
系统 SHALL 只显式设置 4 核心层级，其余使用 MD3 默认值。

#### Scenario: 标题大字
- **WHEN** 渲染 headlineLarge
- **THEN** FontWeight.Bold, 24sp

#### Scenario: 标题中字
- **WHEN** 渲染 titleMedium
- **THEN** FontWeight.SemiBold, 16sp

#### Scenario: 正文
- **WHEN** 渲染 bodyMedium
- **THEN** FontWeight.Normal, 14sp, lineHeight 22sp

#### Scenario: 小标签
- **WHEN** 渲染 labelSmall
- **THEN** FontWeight.Normal, 11sp, letterSpacing 0.5sp

## REMOVED Requirements

### Requirement: DeepBlue 辅助色
**Reason**: 用户新规范未要求 secondary 双色调，改回 MD3 默认。
**Migration**: 所有引用 `DeepBlue` / `DeepBlueLight` / `DarkBackgroundDeep` / `DarkSurfaceDeep` 等的代码改为读取 `MaterialTheme.colorScheme.*`。

### Requirement: 旧 AmberGold 变体系列
**Reason**: primary 改为新色值 `#B87333`，旧变体（`AmberGoldDark`/`AmberGoldLight`/`AmberGoldContainer` 等）不再适用。
**Migration**: 容器色和 onContainer 色改由 MD3 自动计算或使用内联值。