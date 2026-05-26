# Tasks

- [x] Task 1: 调色板与主题升级
  - [x] Color.kt 新增深蓝色常量（DeepBlue, DeepBlueLight 等），调整背景色更柔和
  - [x] Theme.kt DarkColorScheme/LightColorScheme 加入 secondary 蓝色
  - [x] Type.kt 字体层级优化（headlineLarge→Bold, titleLarge→SemiBold）

- [x] Task 2: GlassCard 通用卡片组件
  - [x] 创建 ui/components/GlassCard.kt
  - [x] 封装 ElevatedCard：RoundedCornerShape(16dp) + shadowElevation(2dp) + border(0.5dp, outlineVariant)
  - [x] 提供 GlassCard(contentPadding=16dp) 和 GlassCardSection 变体

- [x] Task 3: 液态毛玻璃导航栏
  - [x] 创建 ui/components/GlassNavigationBar.kt
  - [x] Box 浮动布局：clip + blur + 半透明背景 + 描边 + 阴影
  - [x] 4 个导航项 Row，弹性缩放动画（Animatable spring + animateColorAsState）
  - [x] MainActivity.kt 替换原 NavigationBar 为 GlassNavigationBar
  - [x] 处理安全区 insets

- [x] Task 4: 首页卡片化改造
  - [x] 概览区域（今日支出/本月支出）→ GlassCard 容器
  - [x] 趋势图 + 圆环图 → GlassCardSection 包裹
  - [x] TransactionItem → 卡片行（圆角+间距）
  - [x] 全局间距调整为 20dp/12dp/16dp 标准

- [x] Task 5: 记录页卡片化改造
  - [x] 筛选栏 → GlassCard 容器
  - [x] SwipeableRecordItem 列表项 → 圆角卡片样式（surface 背景 + 12dp 圆角）
  - [x] 全局间距调整
- [x] Task 6: 理财页卡片化改造
  - [x] 总市值概览保持 RoundedCornerShape 优化
  - [x] 持仓列表项 → 统一卡片样式
  - [x] 全局间距调整

- [x] Task 7: 设置页卡片化改造
  - [x] 每个 Section 设置组用 GlassCard 包裹
  - [x] SectionHeader 保持在 Card 外部
  - [x] 全局间距调整

# Task Dependencies
- Task 1（主题）是全局前置，需最先完成
- Task 2（GlassCard）依赖 Task 1
- Task 3（导航栏）依赖 Task 1
- Task 4-7（各页面改造）依赖 Task 2，可部分并行
