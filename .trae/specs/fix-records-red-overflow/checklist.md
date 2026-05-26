# Checklist

- [x] SwipeToDismissBox 外层有 clip(RoundedCornerShape(16.dp)) — L553
- [x] backgroundContent Box 使用 matchParentSize() + clip(16.dp) — L557-L558
- [x] dismissContent Row clip 改为 16.dp 统一圆角 — L574
- [x] LazyColumn 使用 verticalArrangement = spacedBy(6.dp) — L173
- [x] 移除 items 内的 Spacer(4.dp) + HorizontalDivider — 已删除
- [x] 代码静态验证通过