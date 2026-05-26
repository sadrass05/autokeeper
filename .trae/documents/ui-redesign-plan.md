# UI 重设计计划 — 自动记账助手

---

## 视觉主张（Visual Thesis）
> 沉静、精准、专业的财务仪表盘。深色主题为主，单一强调色（琥珀金 #F59E0B），大面积留白与强排版层级，避免卡片堆砌，用空间和数字本身说话。

---

## 内容计划（Content Plan）

### 首页 — 仪表盘（Hero: 本月净收支概览）
- **Hero区**：大字号净收支数字 + 环形对比（支出 vs 收益），占屏幕 35%
- **支撑区**：月支出趋势迷你折线图（MPAndroidChart 嵌入 Compose）
- **详情区**：最近 5 笔交易 — 无卡片，细分割线列表
- **CTA**：快速添加理财持仓入口

### 记录页 — 交易流水
- **顶部**：搜索/筛选栏（按平台、分类、月份）
- **主体**：交易列表 — 无卡片布局，每行左对齐商户+时间，右对齐金额
- **交互**：点击弹出底部 Sheet（非 AlertDialog）编辑分类

### 理财页 — 持仓分析
- **Hero区**：总持仓市值 + 累计收益 + 收益率 三列数字
- **主体**：持仓列表 — 无卡片布局，每行产品名 + 盈亏百分比
- **CTA**：FAB 添加持仓，底部 Sheet 表单替代 Dialog

### 设置页 — 系统配置
- 分组列表布局，每组带标题
- 通知权限、数据库同步、导出设置、关于

---

## 交互主张（Interaction Thesis）
1. **入场动画**：首页数字从 0 滚动到实际值（300ms ease-out）
2. **底部 Sheet**：编辑分类和添加持仓改为 BottomSheet，从底部滑入
3. **页面切换**：淡入淡出过渡，无生硬跳转

---

## 实施步骤

### Step 1: 重写主题系统（Theme.kt）
- 创建自定义 ColorScheme：深色主题 + 琥珀金强调色
- 定义 Typography 层级：Display / Headline / Title / Body / Label
- 移除 themes.xml 中的 Material3 标准主题引用
- 文件：新建 `ui/theme/Theme.kt`、`ui/theme/Color.kt`、`ui/theme/Type.kt`
- 修改：`themes.xml` 改为自定义主题

### Step 2: 重设计首页（HomeScreen.kt）
- 顶部净收支大数字（支出 - 收益 = 净支出/净盈余）
- 嵌入迷你折线图（本月每日支出趋势）
- 最近交易列表：无卡片，Row 布局 + 分割线
- 快速操作入口（添加理财、导出Excel）
- 移除所有 Card 组件，改用 Surface + Column 布局

### Step 3: 重设计记录页（RecordsScreen.kt）
- 顶部筛选栏（平台下拉、分类下拉、月份选择）
- 交易列表：无卡片 Row 布局，左对齐信息，右对齐金额
- 点击弹出 ModalBottomSheet 编辑分类（替代 AlertDialog）
- 左滑删除

### Step 4: 重设计理财页（FinanceScreen.kt）
- 顶部三列概览：总市值 / 累计收益 / 收益率
- 持仓列表：无卡片布局
- FAB 添加持仓 → ModalBottomSheet 表单（替代 AlertDialog）
- 盈亏颜色：正收益绿色，负收益红色

### Step 5: 重设计设置页（SettingsScreen.kt）
- 分组 Section 布局（通知、同步、导出、关于）
- 每行带图标 + 标题 + 副标题
- 底部版本号

### Step 6: 更新 MainActivity.kt 导航栏
- 导航栏图标改为填充/线框双态（选中填充，未选中线框）
- 添加页面切换动画（淡入淡出）

### Step 7: 添加图表组件
- 创建 `ui/components/MiniLineChart.kt` — 封装 MPAndroidChart 迷你折线图
- 创建 `ui/components/SummaryRing.kt` — Canvas 绘制环形对比图

---

## 不修改的文件（保护）
- `app/build.gradle`
- `build.gradle`
- `AndroidManifest.xml`
- `di/` 所有文件
- `data/` 所有文件
- `notification/` 所有文件
- `ocr/` 所有文件
- `excel/` 所有文件
- `network/` 所有文件
- `viewmodel/MainViewModel.kt`（仅在需要新数据时添加字段，不修改现有逻辑）
- 所有 `res/drawable/` 图标文件
- `res/values/strings.xml`

---

## 技术约束（实施时必须遵守）

### 1. StateFlow vs LiveData
- StateFlow → `collectAsStateWithLifecycle()`
- LiveData → `observeAsState()`
- 当前代码已正确使用，继续沿用

### 2. Gradle 版本管理
- 使用前确认变量已在 `build.gradle` 的 `ext` 块中定义
- 优先使用 Compose BOM 统一管理版本（当前已配置 `compose-bom:2024.04.01`）
- 新增依赖版本号通过 `ext` 变量引用，不硬编码

### 3. Hilt 规范
- `@HiltAndroidApp` — Application（已有）
- `@AndroidEntryPoint` — Activity（已有）
- `@HiltViewModel` — ViewModel（已有）
- 三处缺一不可，当前已正确配置

### 4. Composable 存储规范
- 存储 Composable 时**必须用 lambda 包裹**，不能直接调用后存储
- ✅ 正确：`{ HomeScreen() }`
- ❌ 错误：`HomeScreen()`
- 当前 `MainActivity.kt` 已正确使用 lambda 包裹，继续沿用