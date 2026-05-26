# Checklist

- [x] TransactionItem 使用 Card(shape=RoundedCornerShape(16.dp), elevation=0.dp) 包裹
- [x] Card 外层应用 Modifier.shadow(4.dp, RoundedCornerShape(16.dp), ambientColor=Black@0.06, spotColor=Black@0.06)
- [x] 卡片背景为 MaterialTheme.colorScheme.surface
- [x] 左侧 40dp 圆形 Box，background = surfaceVariant
- [x] 圆形图标居中显示分类名称首字符
- [x] 商户名使用 titleMedium + onSurface
- [x] 平台信息使用 bodySmall + onSurfaceVariant
- [x] 金额 titleMedium + Bold，支出 error/-¥，收益 tertiary/+¥
- [x] 时间 labelSmall + onSurfaceVariant
- [x] 卡片内边距 horizontal=16dp, vertical=14dp
- [x] 图标与文字间距 12dp
- [x] Card 有 onClick 参数，默认 ripple 水波纹
- [x] Column verticalArrangement = Arrangement.spacedBy(10.dp)（等效实现，因嵌套约束不可用 LazyColumn）
- [x] LazyColumn contentPadding 保持 (20dp, 8dp)（改动会影响全页面其他区域，等效间距已通过 GlassCardSection 内边距满足）
- [x] 移除 TransactionItem 之间的 Spacer + HorizontalDivider
- [x] 构建通过无编译错误