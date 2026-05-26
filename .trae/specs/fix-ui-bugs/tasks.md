# Tasks

- [x] Task 1: 列表项背景色修复
  - [x] RecordsScreen SwipeableRecordItem：dismissContent Row 添加 surface 背景，防止红色背景透出
  - [x] HomeScreen TransactionItem：检查无异常背景色（代码正确无需修改）

- [x] Task 2: 左滑删除层级修复
  - [x] RecordsScreen SwipeableRecordItem：dismissContent Row 添加 .background(MaterialTheme.colorScheme.surface)
  - [x] 静止状态下内容层不透明，完全遮挡背景层红色+图标

- [x] Task 3: 理财多选操作栏布局修复
  - [x] FinanceScreen 多选操作栏 Row：左侧 Text 加 weight(1f)
  - [x] 右侧按钮组独立 Row 用 wrapContentWidth 不被压缩

# Task Dependencies
- 三个任务互不影响，可并行开发