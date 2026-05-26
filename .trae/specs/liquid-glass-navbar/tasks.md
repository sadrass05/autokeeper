# Tasks

- [x] Task 1: 重写 GlassNavigationBar 液态毛玻璃组件
  - [x] SubTask 1.1: 将容器从 Surface 改为 Box，添加 Modifier.blur(20.dp)
  - [x] SubTask 1.2: 修改 Shape 为 RoundedCornerShape(32.dp)，固定高度 64dp
  - [x] SubTask 1.3: 升级阴影为 shadow(elevation=16.dp, ambientColor=Black.alpha(0.08f))
  - [x] SubTask 1.4: 修改背景透明度：浅色 White.alpha(0.72f)，深色 Color(0xFF1A1A1A).alpha(0.75f)
  - [x] SubTask 1.5: 将 scale 动画从 0.85→1.0 (Animatable) 改为 1.0→1.2 (animateFloatAsState + spring)
  - [x] SubTask 1.6: 保留图标/文字颜色 animateColorAsState 过渡动画
  - [x] SubTask 1.7: 移除不再需要的 Surface 和 clip 包装层

- [x] Task 2: 修改 MainActivity 导航栏为悬浮 overlay 定位
  - [x] SubTask 2.1: 将 Scaffold 底部导航栏移除，改为 Box(Modifier.fillMaxSize()) 外层包裹
  - [x] SubTask 2.2: 在 Box 内使用 Modifier.align(Alignment.BottomCenter) 悬浮 GlassNavigationBar
  - [x] SubTask 2.3: 底部间距改为 12dp.padding(bottom) + navigationBarsPadding()
  - [x] SubTask 2.4: Scaffold contentPadding 移除底部导航栏高度占用

# Task Dependencies
- Task 2 depends on Task 1：MainActivity 需要新的 GlassNavigationBar 接口已就绪