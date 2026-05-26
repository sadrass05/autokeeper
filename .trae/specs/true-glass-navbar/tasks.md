# Tasks

- [x] Task 1: 重写 GlassNavigationBar 为 4 层真实毛玻璃结构
  - [x] SubTask 1.1: 使用 clip(RoundedCornerShape(32.dp)) 包裹整体容器
  - [x] SubTask 1.2: 阴影升级为 shadow(20.dp, spotColor=Black@0.12, ambientColor=Black@0.06)
  - [x] SubTask 1.3: 第1层：Box(matchParentSize) + surface/深色背景 + blur(30.dp)
  - [x] SubTask 1.4: 第2层：Box(matchParentSize) + 渐变磨砂（浅色 White 渐变 / 深色单色）
  - [x] SubTask 1.5: 第3层：Box(matchParentSize) + border(1.dp, 渐变描边, RoundedCornerShape(32.dp))
  - [x] SubTask 1.6: 第4层：Row(matchParentSize, padding) 保持现有导航项逻辑
  - [x] SubTask 1.7: 深浅色适配：根据 background == DarkBackground 切换配色
  - [x] SubTask 1.8: 添加必要 import（Brush 等）

- [x] Task 2: MainActivity Scaffold contentWindowInsets 调整
  - [x] SubTask 2.1: Scaffold 添加 contentWindowInsets = WindowInsets(0)
  - [x] SubTask 2.2: 添加 import androidx.compose.foundation.layout.WindowInsets

- [x] Fix: 移除 BlurEdgeTreatment（BOM 2024.08.00 不支持此 API）

# Task Dependencies
- Task 2 depends on Task 1（导航栏毛玻璃效果需要底层内容延伸才能体现）