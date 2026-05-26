# App 六项改进 - 设计文档

## 1. MiniLineChart 深色模式 X 轴标签修复

**根因**：`AndroidView` 的 `factory` 闭包只在 View 首次创建时执行，`textColor` 在这里设置后，主题切换时 `update` 闭包未同步更新 X 轴标签颜色。

**修复方案**：
- 将 `xAxis.textColor` 的设置从 `factory` 移到 `update` 闭包中
- `update` 闭包每次重绘时同步当前 `MaterialTheme.colorScheme.onSurfaceVariant.toArgb()`
- 同时检查整个文件是否有其他仅在 `factory` 中设置的硬编码颜色

**影响文件**：`ui/components/MiniLineChart.kt`

---

## 2. 手动添加支出 + 记录列表调整

### 2.1 手动添加支出
- RecordsScreen 筛选栏上方新增 `OutlinedButton`"手动添加"
- 点击弹出 `AddExpenseSheet`（ModalBottomSheet），字段：
  - 金额（OutlinedTextField，数字键盘）
  - 分类（ExposedDropdownMenu，从分类列表选择，默认"未分类"）
  - 平台（ExposedDropdownMenu：微信/支付宝/拼多多/其他）
  - 支付渠道（OutlinedTextField，可选）
  - 理财支出标记（Switch）
- 保存时创建 `ExpenseRecord`，设置 `recordedAt = System.currentTimeMillis()`，`notificationId = "manual_${timestamp}"`
- 通过 ViewModel 插入 Room 并刷新列表

### 2.2 记录列表显示
- 标题从 `expense.merchant` 改为 `expense.category`（如"餐饮"、"交通"）
- 副标题保留 `expense.platform · expense.paymentChannel`
- 理财标记的"理财"标签从标题行移到分类标签旁

**影响文件**：`ui/screen/RecordsScreen.kt`

---

## 3. 理财持仓导入（表格 + 自由文本）

**入口**：FinanceScreen 顶部概览卡片右侧增加 `IconButton`（导入图标）

**解析流程**：
1. `ActivityResultContracts.OpenDocument` 选择文件
2. 读取文件内容，先尝试表格解析
3. **表格解析**：按逗号或制表符分隔，期望 ≥6 列（产品名称、平台、买入金额、当前市值、收益、收益率）
4. **自由文本解析**（表格失败时回退）：正则匹配数字金额 + 产品名关键字
   - `(\d+\.?\d*)` 匹配金额
   - "持仓""市值""收益""收益率"等关键字定位
5. 解析结果通过 ViewModel 批量插入 Room
6. Toast 显示成功/失败数量

**影响文件**：`ui/screen/FinanceScreen.kt`、`ui/viewmodel/MainViewModel.kt`、新增 `ui/importdata/FinanceImportParser.kt`

---

## 4. 理财标记自动分类（回归验证）

当前 EditRecordSheet 已实现：Switch ON → `selectedCategory = "理财支出"`。
本次不做代码修改，仅做回归验证确认功能正常。

**影响文件**：无修改，验证 `ui/screen/RecordsScreen.kt` 第 526-536 行

---

## 5. 数据库同步 + 导出 Excel

### 5.1 Flask 后端（新建）
- 创建 `backend/app.py`，依赖 Flask + PyMySQL
- 4 个 API 端点（与 Retrofit `ApiService` 接口匹配）：
  - `POST /api/expenses/sync` — 接收支出数据，写入 MySQL
  - `GET /api/expenses/list` — 查询支出数据
  - `POST /api/finance/sync` — 接收理财数据，写入 MySQL
  - `GET /api/finance/list` — 查询理财数据
- MySQL 连接配置通过环境变量或配置文件
- CORS 开启，允许 Android 端访问

### 5.2 SettingsScreen 接线
- "数据库同步" `onClick` → 调用 `MySqlApi` 推送本地数据
- "导出Excel" `onClick` → 调用 `ExcelExporter.exportToExcel`，Toast 提示结果

**影响文件**：新建 `backend/app.py`、`backend/requirements.txt`，修改 `ui/screen/SettingsScreen.kt`

---

## 6. Excel 导出线程修复

**根因**：`ExcelExporter.exportToExcel` 声明为 `suspend` 但内部无挂起点，在 `Dispatchers.Main` 协程中执行同步 I/O，大数据量时导致 ANR/闪退。

**修复**：
- `ExcelExporter.exportToExcel` 内部用 `withContext(Dispatchers.IO) { }` 包裹所有耗时操作
- 调用处（MainActivity、SettingsScreen）确保使用 `Dispatchers.IO`

**影响文件**：`ui/export/ExcelExporter.kt`、`MainActivity.kt`、`ui/screen/SettingsScreen.kt`
