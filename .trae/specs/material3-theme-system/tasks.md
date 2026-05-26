# Tasks

- [x] Task 1: 重写 Color.kt — 精简调色板
  - [x] SubTask 1.1: 只保留 10 个主题直接引用的颜色常量
  - [x] SubTask 1.2: primary 新色值 Color(0xFFB87333)
  - [x] SubTask 1.3: background/eot 新色值，tertiary 新色值 Color(0xFF43A047)
  - [x] SubTask 1.4: error 新色值 Color(0xFFE53935)
  - [x] SubTask 1.5: 移除所有旧常量（DeepBlue 系列、旧 AmberGold 变体、PositiveGreen/NegativeRed 等）

- [x] Task 2: 重写 Type.kt — 精简字体层级
  - [x] SubTask 2.1: 只显式设置 4 核心层级（headlineLarge/titleMedium/bodyMedium/labelSmall）
  - [x] SubTask 2.2: 其余 9 个 MD3 角色不覆盖，使用默认值

- [x] Task 3: 新建 Shape.kt — 统一圆角系统
  - [x] SubTask 3.1: 创建 4 级 Shapes（small=8dp, medium=12dp, large=16dp, extraLarge=24dp）
  - [x] SubTask 3.2: extraSmall 保持 MD3 默认 4dp

- [x] Task 4: 重写 Theme.kt — 配色方案组装
  - [x] SubTask 4.1: 重写 DarkColorScheme，使用新 Color.kt 常量
  - [x] SubTask 4.2: 重写 LightColorScheme，使用新 Color.kt 常量
  - [x] SubTask 4.3: AutoBookkeeperTheme 添加 shapes = AppShapes 参数
  - [x] SubTask 4.4: 移除不再需要的旧 colorScheme 参数（secondary, container 等）

- [x] Task 5: 迁移下游引用 — 将直接引用旧颜色常量的文件改为 MaterialTheme.colorScheme API
  - 修复 GlassNavigationBar.kt 中深色背景判断值从 `0xFF0B0E14` 改为 `DarkBackground`

# Task Dependencies
- Task 1, 2, 3 相互独立，可并行执行
- Task 4 depends on Task 1（需要新 Color.kt 常量）
- Task 5 depends on Task 1, 2, 3, 4（需要完整主题迁移后才能检查下游引用）