# 液态玻璃导航栏 — 实现计划

## 一、Summary

将 `GlassNavigationBar` 从当前的 4 层半透明叠加重构为 **5 层镜面高光型液态玻璃结构**，同时修复文字裁切问题。仅修改 2 个文件：[GlassNavigationBar.kt](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/components/GlassNavigationBar.kt) 和 [MainActivity.kt](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/MainActivity.kt)。

---

## 二、Current State Analysis

### 2.1 文字裁切根因（精确数据）

```
导航栏 Box height = 64dp
Row padding(vertical=4dp) → 可用 64-8=56dp
Column padding(vertical=8dp) → 可用 56-16=40dp  ← 内容空间
内容：Icon(24dp) + Spacer(2dp) + Text(10sp≈14dp) + Spacer(2dp) + Indicator(2dp) = 44dp
44dp > 40dp → 文字底部被裁切 ❌
```

### 2.2 玻璃效果现状

| 层级 | 当前实现 | 问题 |
|------|---------|------|
| 第1层 模糊扩散 | `surface/深色@0.88` + `blur(30dp)` | 纯色块 blur 自身，无背景穿透感 |
| 第2层 磨砂 | `White/深色渐变` | 单一色阶，缺乏玻璃层次 |
| 第3层 高光 | `border 1dp` 垂直渐变 | 太薄，无镜面反射感 |
| 第4层 内容 | `Row` 导航项 | 空间不足导致裁切 |

---

## 三、Proposed Changes

### 3.1 GlassNavigationBar.kt — 完整重写 GlassNavigationBar 函数

#### 3.1.1 尺寸参数

| 参数 | 旧值 | 新值 | 说明 |
|------|------|------|------|
| 导航栏高度 | `64.dp` | **`80.dp`** | 增大 16dp 解决裁切 + 为玻璃层次留空间 |
| 圆角 | `RoundedCornerShape(32.dp)` | **`RoundedCornerShape(40.dp)`** | 80dp/2 = 40dp 完美胶囊形 |
| 阴影 elevation | `20.dp` | **`24.dp`** | 更高 = 更强悬浮感 |
| Row padding | `vertical=4.dp` | **`vertical=6.dp`** | 内容区 80-12=68dp |
| Column padding | `vertical=8.dp` | **`vertical=10.dp`** | 内容区 68-20=48dp |
| 内容总高 | 44dp → 40dp空间 ❌ | 46dp → 48dp空间 ✅ | 裁切解决 |

#### 3.1.2 5 层液态玻璃结构

```
Box(80dp, clip 40dp, shadow 24dp)
├─ Layer 1: 阴影背景扩散层
│    └─ Box(matchParentSize) + semi-transparent bg + blur(40dp)
│
├─ Layer 2: 主体磨砂渐变层（多色阶）
│    └─ Box(matchParentSize) + Brush.verticalGradient(3色阶)
│
├─ Layer 3: 镜面高光线（0-15% 区域）
│    └─ Box(matchParentSize) + Canvas/drawBehind 绘制顶部高光线
│
├─ Layer 4: 边缘微光描边 + 底部内阴影
│    └─ Box(matchParentSize) + border(1dp, 渐变) + 底部渐变叠加
│
└─ Layer 5: 导航内容
     └─ Row(matchParentSize, padding 8dp/6dp) + GlassNavItem × 4
```

#### 3.1.3 图层详细色值

**深色模式 (DarkBackground = #121212)：**

| 层 | 色值 | 说明 |
|---|------|------|
| Layer 1 扩散底 | `Color(0xFF121212).copy(alpha=0.92f)` + `blur(40.dp)` | 接近背景色的厚底扩散 |
| Layer 2 磨砂主体 | `Brush.verticalGradient(0f→#2C2C2E@0.88, 0.3f→#252528@0.78, 1f→#1C1C1E@0.95)` | 3 色阶模拟玻璃纵深感 |
| Layer 3 镜面高光 | `Brush.verticalGradient(0f→White@0.18, 0.08f→White@0.06, 0.15f→White@0.0)` | 顶部 15% 区域渐变至透明 |
| Layer 4 边缘+内影 | `border(0.5dp, White@0.20→White@0.02)` + 底部 `drawRect(Black@0.15→Transparent)` | 顶部亮线 + 底部厚实内阴影 |

**浅色模式 (WarmBackground = #F5F5F0)：**

| 层 | 色值 |
|---|------|
| Layer 1 扩散底 | `Color(0xFFF5F5F0).copy(alpha=0.85f)` + `blur(40.dp)` |
| Layer 2 磨砂主体 | `Brush.verticalGradient(0f→White@0.92, 0.3f→White@0.78, 1f→#F0EDE8@0.90)` |
| Layer 3 镜面高光 | `Brush.verticalGradient(0f→White@0.90, 0.08f→White@0.25, 0.15f→White@0.0)` |
| Layer 4 边缘+内影 | `border(0.5dp, White@0.95→White@0.15)` + 底部内影 |

#### 3.1.4 GlassNavItem 微调

| 参数 | 旧值 | 新值 | 原因 |
|------|------|------|------|
| Column `padding(vertical)` | `8.dp` | **`10.dp`** | 配合 80dp 高度，上下留白更均衡 |
| Icon-Spacer-Text 间距 | `2.dp` | **`3.dp`** | 文字与图标呼吸感更好 |
| `Spacer` after Text | `2.dp` | **`3.dp`** | 同上 |

#### 3.1.5 新增 Imports

```kotlin
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
```

### 3.2 MainActivity.kt — 底部间距调整

| 修改 | 内容 |
|------|------|
| L125 `padding(bottom=12.dp)` | 改为 `padding(bottom=16.dp)` |
| 其他 | 完全不变 |

---

## 四、Assumptions & Decisions

| # | 决策 | 理由 |
|---|------|------|
| 1 | 使用多层渐变 + blur 模拟玻璃（非截图方案） | 性能最优，不依赖 Bitmap 操作，兼容 minSdk 26 |
| 2 | 导航栏高度 80dp | 用户明确选择，提供充足玻璃层次空间 |
| 3 | 镜面高光用 `drawBehind` Canvas 绘制 | 比额外 Box 层更精确控制高光位置，且不增加布局层级 |
| 4 | 深浅色通过 `background == DarkBackground` 判断 | 与现有模式一致，不改变主题判断逻辑 |
| 5 | `BorderStroke` → `border(brush, shape)` | 统一使用 `border` API，渐变描边需 `Brush` |
| 6 | 保持 `Modifier.blur()` 仅模糊自身 | Compose API 限制，通过多层叠加模拟弥补 |
| 7 | GlassNavItem 结构不变 | 仅调整间距参数，不改动画/交互逻辑 |

---

## 五、Verification Steps

1. 构建 `compileDebugKotlin` 通过无报错
2. 目视检查：导航栏 4 个标签完整显示（不再裁切 "设" 等字的底部）
3. 目视检查：浅色模式下顶部有明显白色镜面反光线
4. 目视检查：深色模式下玻璃有厚实纵深感
5. 检查 80dp 导航栏 + 16dp bottom padding 与系统导航条间距合理
6. 检查缩放动画、颜色过渡、选中指示器均正常
