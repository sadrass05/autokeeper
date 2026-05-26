# UI 全面翻新 Spec

## Why
当前 App 采用扁平化设计，无卡片组件、无圆角、无阴影、导航栏为标准 Material3 样式。需全面升级为现代精致高端视觉效果。

## What Changes
- 底部导航栏改为自定义液态毛玻璃胶囊（高斯模糊 + 半透明 + 描边 + 阴影 + 弹性缩放动画）
- 调色板升级：保留琥珀金主色，引入深蓝辅助色，优化背景/表面色
- 所有页面卡片化：ElevatedCard 替代裸容器，统一 16dp 圆角 + 柔和阴影 + 描边
- 字体层级优化：标题加粗、正文可读性增强
- 全局间距标准化：页面 padding 20dp、卡片间距 12dp、卡片内 padding 16dp

## Impact
- Affected specs: fix-ui-bugs, fix-records-trash-selection
- Affected code: MainActivity.kt, Theme.kt, Color.kt, Type.kt, HomeScreen.kt, RecordsScreen.kt, FinanceScreen.kt, SettingsScreen.kt
- New files: ui/components/GlassNavigationBar.kt, ui/components/GlassCard.kt

---

## ADDED Requirements

### Requirement: 液态毛玻璃导航栏
系统 SHALL 使用自定义导航栏替代 Material3 NavigationBar，具有半透明模糊背景、描边、阴影和弹性缩放动画。

#### Scenario: 导航栏显示
- **WHEN** 用户打开应用
- **THEN** 底部显示胶囊形浮动导航栏
- **AND** 背景为半透明 + 高斯模糊
- **AND** 左右内缩 16dp，底部安全区距离 8dp

#### Scenario: 图标点击动画
- **WHEN** 用户点击导航栏图标
- **THEN** 图标弹性缩放 0.85→1.0（spring 动画）
- **AND** 颜色从灰色渐变到琥珀金（300ms）

#### Scenario: 深浅主题适配
- **WHEN** 用户切换深浅色主题
- **THEN** 导航栏基底色、描边、阴影自动适配

### Requirement: 调色板升级
系统 SHALL 引入深蓝色作为辅助强调色，优化背景和表面色。

#### Scenario: 深色模式
- **WHEN** 深色模式激活
- **THEN** primary=琥珀金、secondary=深蓝(#2563EB)、background=更深黑(#0B0E14)、surface=深蓝灰(#161A22)

#### Scenario: 浅色模式
- **WHEN** 浅色模式激活
- **THEN** primary=琥珀金、secondary=亮蓝(#3B82F6)、background=暖灰白(#F5F5F7)、surface=白色

### Requirement: 页面卡片化
所有页面 SHALL 使用 ElevatedCard 或统一卡片容器替代裸布局容器。

#### Scenario: 首页卡片化
- **WHEN** 查看首页
- **THEN** 概览数字卡片、图表容器、交易列表项均使用卡片容器
- **AND** 卡片统一 16dp 圆角、柔和阴影、半透明描边

#### Scenario: 记录/理财/设置页卡片化
- **WHEN** 查看任意页面
- **THEN** 数据容器使用统一卡片样式
- **AND** 列表项有圆角和间距

### Requirement: 字体层级优化
系统 SHALL 优化字体层级使标题更醒目、正文更可读。

#### Scenario: 标题显示
- **WHEN** 显示页面标题
- **THEN** 使用 Bold 字重

### Requirement: 全局间距标准化
系统 SHALL 使用统一的间距标准。

#### Scenario: 间距标准化
- **WHEN** 渲染任何页面
- **THEN** 页面水平 padding=20dp、卡片间距=12dp、卡片内 padding=16dp
