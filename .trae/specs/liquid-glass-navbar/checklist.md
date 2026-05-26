# Checklist

- [x] GlassNavigationBar 使用 Box 作为容器（非 Surface）
- [x] Modifier.blur(radius=20.dp) 已应用实现毛玻璃模糊
- [x] RoundedCornerShape(32.dp) 胶囊形状已应用
- [x] 导航栏固定高度为 64dp
- [x] 宽度为屏幕宽度减去左右各 16dp margin
- [x] shadow(elevation=16.dp, ambientColor=Black.copy(alpha=0.08f)) 阴影已应用
- [x] 浅色模式背景色为 White.copy(alpha=0.72f)
- [x] 深色模式背景色为 Color(0xFF1A1A1A).copy(alpha=0.75f)
- [x] 外层 1dp BorderStroke，颜色 White.copy(alpha=0.4f)
- [x] 选中图标 scale 动画使用 animateFloatAsState + spring(1.0→1.2)
- [x] 图标和文字颜色使用 animateColorAsState(tween 300ms) 过渡
- [x] MainActivity 导航栏从 Scaffold bottomBar 改为 Box overlay 悬浮
- [x] 导航栏使用 Modifier.align(Alignment.BottomCenter) 定位
- [x] 底部间距为 12dp + navigationBarsPadding()
- [x] 页面内容区域可延伸至屏幕底部（不被导航栏占用高度）
- [x] 深色/浅色主题切换时导航栏样式正确适配
- [x] 构建通过无编译错误