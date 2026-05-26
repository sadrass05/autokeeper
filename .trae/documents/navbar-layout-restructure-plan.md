# 底部导航栏布局架构重构计划

## 问题诊断

### 根本原因
当前 MainActivity.kt 使用 `Box` 叠加 `Scaffold` + `GlassNavigationBar`，但存在以下问题：

1. **Scaffold 的 `bottomBar` 未显式设置**：Scaffold 没有 `bottomBar = {}`，也完全没有定义 bottomBar，导致内容区域无法感知导航栏的存在
2. **各页面底部 padding 不足**：
   - HomeScreen: `contentPadding` 底部仅 `8.dp`
   - RecordsScreen: 无底部 contentPadding
   - FinanceScreen: LazyColumn 无底部 padding（仅 FAB 有 `80.dp`）
   - SettingsScreen: 无底部 padding
3. **层级混乱**：Scaffold 内容渲染到 Box 的全区域，与 GlassNavigationBar 在同一层级，导致红色色块和文字溢出到导航栏下方
4. **玻璃质感不真实**：当前 GlassNavigationBar 使用了多层 Box 嵌套 + blur + 渐变，但缺乏自适应深浅色模式、缺乏 shadow 投影深度、缺乏顶部高光边缘

### 解决方案概述
1. 重构 MainActivity.kt 的 Scaffold 结构，显式设置 `bottomBar = {}`
2. 为每个页面添加 `navigationBarHeight + 安全区` 的底部 padding
3. 完全重写 GlassNavigationBar 组件，采用深浅色自适应设计
4. 优化 NavItem 组件，确保图标和文字完整显示

---

## 实施步骤

### 步骤 1：重构 MainActivity.kt 布局架构

**文件**: `app/src/main/java/com/example/autobookkeeper/MainActivity.kt`

**具体修改**:

1. 新增导入：
   - `androidx.compose.foundation.layout.navigationBarsPadding`（已有）
   - `androidx.compose.foundation.layout.WindowInsets`（已有）
   - `androidx.compose.foundation.layout.asPaddingValues`（需新增）
   - `androidx.compose.foundation.layout.calculateBottomPadding`（需新增）

2. 重构 Scaffold 结构：
   ```kotlin
   // 旧结构（删除）：
   Box(Modifier.fillMaxSize()) {
       Scaffold(
           containerColor = MaterialTheme.colorScheme.background,
           contentWindowInsets = WindowInsets(0)
       ) { padding ->
           Box(modifier = Modifier.fillMaxSize().padding(padding)) {
               Crossfade(targetState = selectedScreen) { screen ->
                   screens[screen].second()
               }
           }
       }
       GlassNavigationBar(
           items = navItems,
           selectedIndex = selectedScreen,
           onItemSelected = { selectedScreen = it },
           modifier = Modifier
               .align(Alignment.BottomCenter)
               .padding(bottom = 16.dp)
               .navigationBarsPadding()
       )
   }
   ```

   改为：
   ```kotlin
   // 新结构：
   Scaffold(
       modifier = Modifier.fillMaxSize(),
       containerColor = MaterialTheme.colorScheme.background,
       contentWindowInsets = WindowInsets(0, 0, 0, 0),
       bottomBar = {}  // 关键：显式设置空 bottomBar，让 Scaffold 不为内容区添加底部 padding
   ) { innerPadding ->
       Box(
           modifier = Modifier
               .fillMaxSize()
               .padding(top = innerPadding.calculateTopPadding())
       ) {
           // 第一层：页面内容区域
           Box(modifier = Modifier.fillMaxSize()) {
               Crossfade(targetState = selectedScreen) { screen ->
                   screens[screen].second()
               }
           }

           // 第二层：导航栏悬浮层，放在 Box 最后确保在最上方
           GlassNavigationBar(
               items = navItems,
               selectedIndex = selectedScreen,
               onItemSelected = { selectedScreen = it },
               modifier = Modifier.align(Alignment.BottomCenter)
           )
       }
   }
   ```

3. 关键点：
   - `bottomBar = {}` 让 Scaffold 知道底部有 bar，但不渲染任何内容（只用于 insets 计算）
   - Box 内部用 `innerPadding.calculateTopPadding()` 只处理顶部状态栏
   - 导航栏作为 Box 的第二层子元素，天然覆盖在内容之上
   - 移除外层 Box，简化层级

### 步骤 2：完全重写 GlassNavigationBar 组件

**文件**: `app/src/main/java/com/example/autobookkeeper/ui/components/GlassNavigationBar.kt`

**删除的导入**（不再使用）：
- `blur` — 使用 shadow + clip 替代
- `scale` — 简化动画
- `drawBehind` — 使用 border 替代手绘
- `Size`, `CornerRadius`, `Stroke` — 不再手绘
- `DarkBackground`, `DarkSurfaceVariant`, `WarmSurfaceVariant` — 使用 MaterialTheme 自适应

**新增的导入**：
- `androidx.compose.foundation.isSystemInDarkTheme` — 深浅色模式检测
- `androidx.compose.foundation.layout.navigationBarsPadding` — 系统导航条安全区

**核心组件结构**：

```kotlin
@Composable
fun GlassNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 玻璃背景层（matchParentSize 确保与内容层等大）
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.82f)
                    else Color.White.copy(alpha = 0.78f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.04f)
                        ) else listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.30f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // 导航项内容层
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                NavItem(
                    item = item,
                    isSelected = selectedIndex == index,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}
```

**设计要点**：
- **32dp 圆角**：比之前的 40dp 更紧凑，视觉效果更精致
- **shadow 投影**：24dp elevation 营造悬浮感
- **clip + background**：clip 裁剪圆角，background 设置半透明底色
- **border 渐变**：从顶部白到透明，模拟玻璃顶部高光边缘
- **深浅色自适应**：`isSystemInDarkTheme()` 自动适配颜色
- **navigationBarsPadding**：在外层 Box 上处理系统导航条，确保不被系统手势条遮挡

### 步骤 3：重写 NavItem 组件

**具体修改**：

```kotlin
@Composable
fun RowScope.NavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        animationSpec = tween(300),
        label = "iconColor"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                if (isSelected) item.selectedIcon else item.icon
            ),
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
```

**关键变化**：
- **移除 scale 动画**：简化动画，减少视觉干扰
- **移除 AnimatedVisibility indicator**：底部指示条不再需要，选中状态通过颜色区分即可
- **移除 padding(vertical = 10.dp)**：依赖 Column 的 `fillMaxHeight` + `Arrangement.Center` 自动居中
- **移除单独的 textColor 动画**：文字颜色与图标颜色统一管理
- **新增 maxLines=1 + overflow=Clip**：确保长文字不溢出

### 步骤 4：为每个页面添加底部安全间距

**导航栏占位高度计算**：
```
导航栏总高度 = vertical padding(12dp × 2) + Row 高度(64dp) = 88dp
```

但由于使用了 `navigationBarsPadding()`，系统手势条已由导航栏组件处理。
页面只需为导航栏胶囊本身留出空间：**64dp（内容高度）+ 24dp（上下 padding）= 88dp**。

#### 4.1 HomeScreen (`HomeScreen.kt`)

**修改 LazyColumn 的 contentPadding**：
```kotlin
// 旧：
contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)

// 新：
contentPadding = PaddingValues(
    start = 20.dp, end = 20.dp,
    top = 8.dp,
    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
)
```

需要新增导入：
- `androidx.compose.foundation.layout.WindowInsets`
- `androidx.compose.foundation.layout.asPaddingValues`
- `androidx.compose.foundation.layout.calculateBottomPadding`

#### 4.2 RecordsScreen (`RecordsScreen.kt`)

**修改外层 Column 的 bottom padding**：

当前 RecordsScreen 的 LazyColumn 没有明确的 contentPadding。需要在 LazyColumn 或外层 Column 添加：

```kotlin
// 在外层 Column 添加底部 padding：
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
) {
```

或者更优雅的方式：为 LazyColumn 添加 contentPadding：
```kotlin
// 找到 RecordsScreen 中的 LazyColumn，修改其 contentPadding
contentPadding = PaddingValues(
    start = 16.dp, end = 16.dp,
    top = 6.dp,
    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
)
```

需要新增导入：同上。

#### 4.3 FinanceScreen (`FinanceScreen.kt`)

**修改 LazyColumn 的 bottom padding**：

```kotlin
// 旧（LazyColumn modifier 链）：
.padding(padding)
.statusBarsPadding()
.padding(horizontal = 16.dp)

// 在 LazyColumn 的 modifier 中添加底部 padding，或在 contentPadding 中设置：
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .statusBarsPadding()
        .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    verticalArrangement = Arrangement.spacedBy(0.dp)
)
```

同时移除 FAB 上的 `padding(bottom = 80.dp)`：
```kotlin
// 旧：
FloatingActionButton(
    onClick = { showActionSheet = true },
    modifier = Modifier
        .padding(bottom = 80.dp)
        .navigationBarsPadding(),
    ...
)

// 新：
FloatingActionButton(
    onClick = { showActionSheet = true },
    modifier = Modifier.navigationBarsPadding(),
    ...
)
```

因为现在 Scaffold 有 `bottomBar = {}`，Scaffold 会自动为 FAB 计算底部安全间距。

#### 4.4 SettingsScreen (`SettingsScreen.kt`)

**修改 Column 的 bottom padding**：

```kotlin
// 旧：
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
)

// 新：
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(
            bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
        .verticalScroll(rememberScrollState())
)
```

需要新增导入：同上。

---

## 汇总：涉及修改的文件

| 文件 | 修改类型 | 关键改动 |
|------|---------|---------|
| `MainActivity.kt` | 架构重构 | Scaffold 添加 `bottomBar={}`，Box overlay 结构，移除旧 GlassNavigationBar 调用方式 |
| `GlassNavigationBar.kt` | 完全重写 | 移除 blur/drawBehind/scale，新增 shadow/clip/border/深浅色自适应，简化 NavItem |
| `HomeScreen.kt` | 底部 padding | LazyColumn contentPadding 底部增加 88dp |
| `RecordsScreen.kt` | 底部 padding | 外层 Column 或 LazyColumn 底部增加 88dp |
| `FinanceScreen.kt` | 底部 padding + FAB 调整 | LazyColumn 底部增加 88dp，移除 FAB 手动 bottom padding |
| `SettingsScreen.kt` | 底部 padding | Column 底部增加 88dp |

## 预期效果
1. 红色色块不再溢出到导航栏下方
2. "¥5.50" 等页面内容文字不再出现在导航栏区域
3. 导航栏真正悬浮覆盖在内容之上，层级清晰
4. 玻璃质感真实：投影深度 + 半透明底色 + 顶部高光 + 深浅色自适应
5. 每个页面底部有足够的滚动空间，最后一项不会被导航栏遮挡
6. FAB 位置由 Scaffold 自动管理，不再手动 hardcode