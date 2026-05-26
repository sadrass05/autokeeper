# Tasks

- [x] Task 1: 修复 GlassNavItem 图标颜色、布局约束和选中指示器
  - [x] SubTask 1.1: 未选中图标 tint 改为 onSurface.copy(alpha = 0.6f)
  - [x] SubTask 1.2: Column 新增 Modifier.fillMaxHeight() 防止高度塌陷
  - [x] SubTask 1.3: padding(vertical) 从 6.dp 调整为 8.dp
  - [x] SubTask 1.4: 新增 AnimatedVisibility + Box(2dp 高, 24dp 宽, RoundedCornerShape(1.dp)) 选中指示器
  - [x] SubTask 1.5: 添加必要 import（AnimatedVisibility、fillMaxHeight、width、clip）

# Task Dependencies
- 无依赖，单一任务