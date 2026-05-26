# Tasks

- [x] Task 1: 修复 HomeScreen 图表切换按钮不可见
  - [x] SubTask 1.1: 替换 IconButton + vectorResource 为 FilledTonalButton + Text("折线"/"柱状")
  - [x] SubTask 1.2: 选中态 containerColor = primary.copy(alpha=0.15f)，未选中 Color.Transparent
  - [x] SubTask 1.3: 移除不需要的 import（R, Icon, IconButton, ImageVector, vectorResource）
  - [x] SubTask 1.4: 添加 FilledTonalButton import

# Task Dependencies
- 无依赖，单一文件修复