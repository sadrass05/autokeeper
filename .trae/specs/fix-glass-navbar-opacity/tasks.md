# Tasks

- [x] Task 1: 修复 GlassNavigationBar 第1层模糊扩散层背景透明度
  - [x] SubTask 1.1: 将 blurBg 变量拆分为独立的半透明色值用于模糊层
  - [x] SubTask 1.2: 浅色模式：background = Color.White.copy(alpha = 0.5f)
  - [x] SubTask 1.3: 深色模式：background = Color(0xFF1C1C1E).copy(alpha = 0.5f)

# Task Dependencies
- 无依赖，单一文件修复