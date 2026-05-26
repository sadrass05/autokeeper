# 多格式理财数据导入 — 实现计划

## 一、Summary

重构 FinanceScreen 的导入体验：删除右上角列表图标按钮，将"+" FAB 改为弹出 BottomSheet 选项面板（手动添加 | 文件导入），新增支持 CSV / Excel (.xlsx/.xls) / TXT 三种格式的理财持仓批量导入，含预览确认步骤。

**只修改 2 个文件**：[FinanceScreen.kt](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt) 和 [FinanceDao.kt](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/data/dao/FinanceDao.kt)。

---

## 二、Current State Analysis

### 2.1 FinanceScreen 现状

| 组件 | 行号 | 现状 |
|------|------|------|
| 右上角导入图标按钮 | L183-194 | `IconButton` + `Icons.AutoMirrored.Filled.List`，点击调 `filePicker.launch("*/*")` |
| 文件选择器 launcher | L99-121 | `ActivityResultContracts.OpenDocument`，回调调 `FinanceImportParser.parse()` |
| FAB "+" | L127-138 | 点击 `showAddSheet = true`，弹出 `AddFinanceSheet` 表单 |
| 添加表单 BottomSheet | L547-712 | 6 个输入框 + 保存按钮，手动添加单条 `FinancePosition` |

### 2.2 数据层现状

| 层 | 方法 | 备注 |
|----|------|------|
| FinanceDao | `insert(position): Long` | 只支持单条插入 |
| FinanceDao | `deleteByIds(ids: List<Long>)` | 已有批量删除，无批量插入 |
| FinanceRepository | `upsertPosition(position)` | 逐条 upsert |
| MainViewModel | `addFinancePositions(parsedList)` | 逐条调 `upsertPosition` |

### 2.3 FinanceImportParser 现状

- 仅支持 TXT 文本格式（制表符/逗号分割 + 自由文本正则提取）
- 不支持 Excel（.xlsx/.xls）
- 不支持 CSV 头行跳过
- 不支持 `.txt` 键值对多行格式
- 返回 `ParsedPosition` 中间类型

### 2.4 已有资源

| 资源 | 可用性 |
|------|--------|
| Apache POI 依赖 (5.2.5) | ✅ 已在 build.gradle 配置 |
| ImportManager.kt (POI Excel 解析参考) | ✅ 存在，用于支出记录导入 |
| ImportResult 数据类 | ✅ 存在 `ui/importdata/ImportResult.kt` |
| ImportResultDialog 组件 | ✅ 存在但未被使用 |

---

## 三、Proposed Changes

### 3.1 FinanceScreen.kt — 重构（核心文件）

#### 3.1.1 删除项

| 删除内容 | 行号 | 说明 |
|----------|------|------|
| `val filePicker = rememberLauncherForActivityResult(...)` | L99-121 | 右上角导入按钮的文件选择器 |
| `IconButton(onClick = { filePicker.launch(...) })` | L183-194 | 右上角列表图标按钮及其外层 `Box(contentAlignment = TopEnd)` 的 `TopEnd` 定位：将统计卡片 `GlassCard` 移出 `Box`，直接放在 item 中 |
| `import ...automirrored.filled.List` | L27 | 删除未使用的导入 |
| `import ...ActivityResultContracts` | L5-6 | 迁移到新位置 |

**注意**：filePicker 将被新的解析器文件选择器替代（在 ActionSheet 中触发），但新 launcher 仍使用 `ActivityResultContracts.OpenDocument`。

#### 3.1.2 FAB 重构

**旧代码** (L127-138):
```kotlin
FloatingActionButton(
    onClick = { showAddSheet = true },
    ...
)
```

**新代码**:
```kotlin
FloatingActionButton(
    onClick = { showActionSheet = true },
    ...
)
```

新增状态变量：
```kotlin
var showActionSheet by remember { mutableStateOf(false) }
var showImportPreview by remember { mutableStateOf(false) }
var importPreviewData by remember { mutableStateOf<List<FinancePosition>>(emptyList()) }
var importPreviewTotal by remember { mutableStateOf(0) }
var importPreviewSkipped by remember { mutableStateOf(0) }
```

#### 3.1.3 新增：FinanceActionSheet（BottomSheet 选项面板）

在 `showActionSheet` 为 true 时显示：

```kotlin
if (showActionSheet) {
    ModalBottomSheet(
        onDismissRequest = { showActionSheet = false },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("添加理财持仓", style = MaterialTheme.typography.titleLarge)

            // 选项 1: 手动添加
            OutlinedButton(
                onClick = {
                    showActionSheet = false
                    showAddSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column { Text("手动添加"); Text("填写表单添加单条持仓", style=labelSmall) }
            }

            // 选项 2: 文件导入
            OutlinedButton(
                onClick = {
                    showActionSheet = false
                    fileImportLauncher.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/plain",
                        "*/*"
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column { Text("文件导入"); Text("从Excel/CSV/TXT批量导入", style=labelSmall) }
            }
        }
    }
}
```

#### 3.1.4 新增：文件导入解析器 (内联函数)

在 FinanceScreen 文件底部（`AddFinanceSheet` 之前）新增解析函数，支持 4 种格式：

```kotlin
private data class FinanceParseResult(
    val positions: List<FinancePosition>,
    val skipped: Int = 0
)

private suspend fun parseFinanceFile(context: Context, uri: Uri): FinanceParseResult {
    val fileName = getFileName(context, uri).lowercase()
    return when {
        fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> parseExcel(context, uri)
        fileName.endsWith(".csv") -> parseCsv(context, uri)
        fileName.endsWith(".txt") -> parseTxt(context, uri)
        else -> parseCsv(context, uri) // fallback to CSV-style parsing
    }
}
```

**Excel 解析** (`parseExcel`):
- 使用 `WorkbookFactory.create(inputStream)` (POI)
- 取第一个 sheet，逐行读取
- 第1行作为表头（跳过），从第2行开始解析
- 单元格索引：0=产品名称, 1=平台, 2=买入金额, 3=当前市值, 4=收益, 5=收益率
- 金额单元格处理：字符串类型去除 `¥`/`￥` 符号，数值类型直接 `getNumericCellValue()`
- `getCellString()` 复用 `ImportManager.kt` 的模式

**CSV 解析** (`parseCsv`):
- `BufferedReader.readLine()` 逐行读取
- 第1行跳过（表头）
- 每行按 `,` 分割，字段索引同 Excel
- 金额去除 `¥`/`￥` 后 `toDoubleOrNull() ?: 0.0`

**TXT 解析** (`parseTxt`):
- 检测格式：若第一行含 `:` 则为键值对格式，否则为制表符/逗号格式
- 键值对格式：按空行分隔记录，每行 `字段名:值` 解析 6 个字段
- 表格式：复用现有 `FinanceImportParser.parseTable()` 逻辑，额外跳过含"产品名称"表头的行

**通用错误处理**：
- 每行解析异常时 `skipped++`，继续下一行
- 金额字段非数字时设为 0.0
- 文件读取异常时返回空列表 + skipped=0

#### 3.1.5 新增：导入预览界面 (ImportPreviewSheet)

```kotlin
if (showImportPreview) {
    ModalBottomSheet(
        onDismissRequest = { showImportPreview = false },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("导入预览", style = MaterialTheme.typography.titleLarge)

            Text(
                "检测到 ${importPreviewTotal} 条记录" +
                if (importPreviewSkipped > 0) "，跳过 ${importPreviewSkipped} 条" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 预览列表（最多 5 条）
            importPreviewData.take(5).forEach { position ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(position.productName, style = titleSmall)
                        Text(position.platform, style = labelSmall, color = onSurfaceVariant)
                    }
                    Text("¥${"%.2f".format(position.currentValue)}", style = titleMedium, color = primary)
                }
                HorizontalDivider(color = outlineVariant)
            }

            if (importPreviewData.size > 5) {
                Text("... 还有 ${importPreviewData.size - 5} 条", color = onSurfaceVariant)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(12.dp)) {
                OutlinedButton(onClick = { showImportPreview = false }, Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        showImportPreview = false
                        scope.launch {
                            viewModel.addFinancePositionsFromList(importPreviewData)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "成功导入 ${importPreviewTotal} 条", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    Modifier.weight(1f)
                ) {
                    Text("确认导入 ${importPreviewData.size} 条")
                }
            }
        }
    }
}
```

#### 3.1.6 新增：文件选择器 launcher（替换旧的）

```kotlin
val fileImportLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let {
        coroutineScope.launch(Dispatchers.IO) {
            val result = parseFinanceFile(context, uri)
            withContext(Dispatchers.Main) {
                if (result.positions.isEmpty()) {
                    Toast.makeText(context, "未能解析到有效数据", Toast.LENGTH_SHORT).show()
                } else {
                    importPreviewData = result.positions
                    importPreviewTotal = result.positions.size
                    importPreviewSkipped = result.skipped
                    showImportPreview = true
                }
            }
        }
    }
}
```

#### 3.1.7 新增 Import

```kotlin
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Edit
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader
```

#### 3.1.8 统计卡片布局简化

删除右上角图标按钮的同时，将 `Box(contentAlignment = TopEnd)` 改为直接放 `GlassCard`：
```kotlin
// 旧 (L148-194):
item {
    Box(contentAlignment = Alignment.TopEnd) {
        GlassCard(...) { /* 统计列 */ }
        IconButton(...) { /* 导入图标 */ }
    }
}

// 新:
item {
    GlassCard(contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = SpaceEvenly) {
            // 4 个 StatColumn（完全不变）
        }
    }
}
```

### 3.2 FinanceDao.kt — 新增批量插入方法

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertPositions(positions: List<FinancePosition>)
```

### 3.3 MainViewModel.kt — 新增批量导入方法

```kotlin
fun addFinancePositionsFromList(positions: List<FinancePosition>) {
    viewModelScope.launch {
        financeRepository.insertPositions(positions)
    }
}
```

### 3.4 FinanceRepository.kt — 新增批量插入方法

```kotlin
suspend fun insertPositions(positions: List<FinancePosition>) {
    financeDao.insertPositions(positions)
}
```

---

## 四、Assumptions & Decisions

| # | 决策 | 理由 |
|---|------|------|
| 1 | Excel 解析内联在 FinanceScreen 文件中（非独立 Parser 文件） | 保持 FinanceImportParser 简洁的现有风格，新解析器紧密耦合于 FinanceScreen 的导入流程 |
| 2 | 使用 `OnConflictStrategy.REPLACE` 批量插入 | 按用户要求，批量插入覆盖冲突行 |
| 3 | 文件类型通过扩展名判断 (`.xlsx`/`.csv`/`.txt`) | 不依赖 MIME 类型（部分文件管理器返回 `application/octet-stream`） |
| 4 | 保留现有 `FinanceImportParser.kt` 不动 | 避免影响其他可能引用它的代码 |
| 5 | TXT 键值对格式：空行分隔记录 | 用户明确指定 |
| 6 | 预览最多显示 5 条 | 用户明确选择"简洁预览" |
| 7 | ActionSheet 用 `ModalBottomSheet` | 用户明确选择 BottomSheet 方案 |

---

## 五、Files Changed

| 文件 | 操作 | 变更量 |
|------|------|--------|
| `ui/screen/FinanceScreen.kt` | 重构 | 删除 ~40 行，新增 ~200 行 |
| `data/dao/FinanceDao.kt` | 新增方法 | +5 行 |
| `data/repository/FinanceRepository.kt` | 新增方法 | +3 行 |
| `ui/viewmodel/MainViewModel.kt` | 新增方法 | +5 行 |

---

## 六、Verification Steps

1. 构建 `compileDebugKotlin` 通过无报错
2. 点击"+"：弹出 BottomSheet 选项面板，含"手动添加"和"文件导入"两个按钮
3. "手动添加"：跳转到原有 AddFinanceSheet 表单
4. "文件导入" → 选择 .csv / .xlsx / .txt 文件
5. CSV/Excel 解析：预览显示正确产品名+金额，确认导入后数据库有对应记录
6. TXT 键值对格式：空行分隔的多条记录被正确解析
7. 错误行：金额非数字时设为 0.0，不中断解析
8. 右上角列表图标：完全消失，无残留代码
