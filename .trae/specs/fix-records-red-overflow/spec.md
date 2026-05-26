# 交易记录列表红色溢出修复 Spec

## Why
SwipeToDismissBox 的 backgroundContent（红色 error 背景）未受 clip 约束，从卡片底部直线溢出到下一个列表项的顶部区域，形成红色矩形视觉异常。

## What Changes
- SwipeToDismissBox 外层添加 clip(RoundedCornerShape(16.dp)) 约束所有子元素
- backgroundContent Box 使用 matchParentSize() 替代 fillMaxSize()，同样 clip 16dp
- dismissContent Row 圆角从 12dp 改为 16dp，与整体统一
- LazyColumn 移除 Spacer + HorizontalDivider 分隔，改用 spacedBy(6.dp)

## Impact
- Affected code: RecordsScreen.kt（SwipeableRecordItem + LazyColumn items）
- New files: 无

---

## MODIFIED Requirements

### Requirement: SwipeableRecordItem 裁剪约束
系统 SHALL 在 SwipeToDismissBox 最外层使用 clip(16.dp) 裁剪，防止子元素溢出。

#### Scenario: 红色背景裁剪
- **WHEN** 左滑显示删除操作
- **THEN** error 背景色在 16dp 圆角内精确裁剪
- **AND** 不会溢出到相邻列表项区域

### Requirement: LazyColumn 间距统一
系统 SHALL 使用 Arrangement.spacedBy(6.dp) 替代手动 Spacer + Divider。

#### Scenario: 列表项间距
- **WHEN** 渲染交易记录列表
- **THEN** item 之间使用 6dp 统一间距
- **AND** 无 Spacer 和 HorizontalDivider 残留