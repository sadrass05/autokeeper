# Checklist

- [x] GlassNavigationBar 外层使用 clip(RoundedCornerShape(32.dp)) 包裹
- [x] 阴影为 shadow(elevation=20.dp, spotColor=Black@0.12, ambientColor=Black@0.06)
- [x] 第1层：Box(matchParentSize) + background + blur(30.dp)
- [x] 第2层：Box(matchParentSize) + 渐变/半透明磨砂
- [x] 第3层：Box(matchParentSize) + border(1.dp, 渐变描边)
- [x] 第4层：Row(matchParentSize) 包含导航项
- [x] 浅色模糊层 background = surface
- [x] 深色模糊层 background = Color(0xFF1C1C1E)
- [x] 浅色磨砂层渐变 White@0.75 → White@0.60
- [x] 深色磨砂层 Color(0xFF2C2C2E)@0.80
- [x] 浅色高光描边 White@0.8 → White@0.1
- [x] 深色高光描边 White@0.12 → White@0.04
- [x] 深浅色根据 background == DarkBackground 自动切换
- [x] GlassNavItem 代码完全不变
- [x] MainActivity Scaffold 有 contentWindowInsets = WindowInsets(0)
- [x] 构建通过无编译错误