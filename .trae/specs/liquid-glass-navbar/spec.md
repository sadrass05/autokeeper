# 液态毛玻璃底部导航栏 Spec

## Why
当前 GlassNavigationBar 缺少真正的 Blur 模糊效果、胶囊形状不够圆润、阴影较弱、动画不够生动。需要升级为真正的液态毛玻璃组件，实现背景高斯模糊 + 强阴影 + 弹簧缩放动画 + 悬浮定位。

## What Changes
- 重写 GlassNavigationBar：Box 容器 + Modifier.blur(20.dp) 真毛玻璃 + RoundedCornerShape(32.dp) + 高度 64dp
- 阴影升级：shadow(elevation=16.dp, ambientColor=Black.alpha(0.08f))
- 缩放动画改为 1.0→1.2 弹簧动画（animateFloatAsState + spring）
- 导航栏从 Scaffold bottomBar 改为 Box overlay 悬浮定位，不占用内容区域高度
- 底部安全距离改为 12dp + navigationBarsPadding()
- **BREAKING**: MainActivity 布局结构从 Scaffold bottomBar 改为 Box overlay

## Impact
- Affected specs: ui-overhaul
- Affected code: GlassNavigationBar.kt, MainActivity.kt
- New files: 无

---

## ADDED Requirements

### Requirement: 液体毛玻璃背景
系统 SHALL 使用 Modifier.blur(radius=20.dp) 对导航栏背景进行高斯模糊。

#### Scenario: 模糊效果生效
- **WHEN** 导航栏渲染在内容之上
- **THEN** 背景呈现毛玻璃模糊效果（API 31+）
- **AND** 低版本设备优雅降级无模糊

### Requirement: 胶囊形状升级
系统 SHALL 使用 RoundedCornerShape(32.dp) 和固定高度 64dp 实现更圆润的胶囊形状。

#### Scenario: 导航栏尺寸
- **WHEN** 渲染导航栏
- **THEN** 宽度 = 屏幕宽度 - 左右各 16dp margin
- **AND** 高度 = 64dp
- **AND** Shape = RoundedCornerShape(32.dp)

### Requirement: 增强阴影
系统 SHALL 使用 16dp 阴影和 ambientColor 实现悬浮感。

#### Scenario: 阴影渲染
- **WHEN** 导航栏显示
- **THEN** shadow(elevation=16.dp, ambientColor=Black.copy(alpha=0.08f))
- **AND** 导航栏视觉上悬浮于内容之上

### Requirement: 液态缩放动画
系统 SHALL 使用 animateFloatAsState 对选中图标实现 1.0→1.2 弹簧缩放。

#### Scenario: 选中图标缩放
- **WHEN** 用户点击导航项使其选中
- **THEN** 图标 scale 从 1.0 动画至 1.2（spring(dampingRatio=0.5f, stiffness=300f)）
- **AND** 未选中图标 scale 回到 1.0

#### Scenario: 颜色过渡
- **WHEN** 导航项选中状态变化
- **THEN** 图标和文字颜色使用 animateColorAsState 300ms tween 过渡

### Requirement: 悬浮定位
系统 SHALL 使用 Box overlay 方式使导航栏悬浮在 Scaffold 内容之上。

#### Scenario: 不占用内容空间
- **WHEN** 导航栏悬浮渲染
- **THEN** 使用 Modifier.align(Alignment.BottomCenter) 定位
- **AND** 不通过 Scaffold bottomBar 传递，内容区域可延伸至屏幕底部
- **AND** 底部安全距离 = 12dp + navigationBarsPadding()

## MODIFIED Requirements

### Requirement: 毛玻璃导航栏（替代原 ui-overhaul 版本）
系统 SHALL 使用液体毛玻璃组件替代原有 GlassNavigationBar。

#### Scenario: 主题适配
- **WHEN** 切换深浅色主题
- **THEN** 背景色自动适配：浅色 White.alpha(0.72f)，深色 Color(0xFF1A1A1A).alpha(0.75f)
- **AND** 边框色：Color.White.copy(alpha=0.4f)

#### Scenario: 导航栏显示
- **WHEN** 用户打开应用
- **THEN** 底部显示液态毛玻璃胶囊导航栏
- **AND** 背景模糊 + 半透明 + 描边 + 阴影 + 悬浮定位