# Checklist

- [x] 未选中图标 tint = onSurface.copy(alpha = 0.6f)
- [x] 选中图标 tint = primary
- [x] Column 有 Modifier.fillMaxHeight()
- [x] padding(vertical) = 8.dp
- [x] 选中指示器：24dp 宽 × 2dp 高，RoundedCornerShape(1.dp)，primary 色
- [x] 指示器使用 AnimatedVisibility(visible = isSelected)
- [x] 构建通过无编译错误