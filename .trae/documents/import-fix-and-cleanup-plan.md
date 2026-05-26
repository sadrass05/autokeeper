# 导入数据修复：清理脏数据 + 修复时间解析 + 手动添加日期选择器

## 根因分析

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 1 | 导入 CSV 时所有 `recordedAt` 都设为 `System.currentTimeMillis()` | [ImportManager.kt:80](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/importdata/ImportManager.kt#L80) / [ImportManager.kt:119](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/importdata/ImportManager.kt#L119) | 历史数据全部归到今天 |
| 2 | 手动添加 `recordedAt = now` 无日期选择 | [RecordsScreen.kt:543-551](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/screen/RecordsScreen.kt#L543-L551) | 手动补录历史数据只能归到今天 |
| 3 | 脏数据需要安全清理 | 新增功能 | 今天误导入的记录需要安全删除 |

---

## 变更范围

### 修改的文件（5 个）

| 文件 | 变更 |
|------|------|
| `data/dao/ExpenseDao.kt` | 新增 `deleteImportedDataOnDay()` DAO 方法 |
| `data/repository/ExpenseRepository.kt` | 新增 `deleteImportedDataOnDay()` 封装 |
| `ui/importdata/ImportManager.kt` | 修复 CSV/TXT 导入的 `recordedAt` 解析 |
| `ui/screen/RecordsScreen.kt` | AddExpenseSheet 添加 DatePicker + TimePicker |
| `ui/screen/SettingsScreen.kt` | 添加"清理错误导入数据"按钮 + 确认对话框 |

---

## 具体变更

### Step 1：ExpenseDao.kt — 新增安全删除 DAO

**文件**：`app/src/main/java/com/example/autobookkeeper/data/dao/ExpenseDao.kt`

在 DAO 接口末尾（`getAllExpensesList()` 后）新增：

```kotlin
@Query("""
    DELETE FROM expenses
    WHERE recordedAt >= :startOfDay
    AND recordedAt <= :endOfDay
    AND (notificationId LIKE 'import_%'
         OR notificationId LIKE 'manual_%'
         OR notificationId = '')
    AND isDeleted = 0
""")
suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int
```

安全约束：
- `isDeleted = 0` — 只操作未删除的记录
- `notificationId LIKE 'import_%'` — 匹配 CSV/TXT 导入（`import_csv_*` / `import_txt_*`）
- `notificationId LIKE 'manual_%'` — 匹配手动添加无日期选择的
- `recordedAt` 范围限制在今天 — 历史数据不动

### Step 2：ExpenseRepository.kt — 新增封装

**文件**：`app/src/main/java/com/example/autobookkeeper/data/repository/ExpenseRepository.kt`

```kotlin
suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int {
    return expenseDao.deleteImportedDataOnDay(startOfDay, endOfDay)
}
```

### Step 3：SettingsScreen.kt — 新增清理按钮

**文件**：`app/src/main/java/com/example/autobookkeeper/ui/screen/SettingsScreen.kt`

在"导入数据"入口行下方（L212-213 之间）新增一个 GlassCard + SettingsRow：

```kotlin
GlassCard(contentPadding = PaddingValues(0.dp)) {
    SettingsRow(
        icon = Icons.Default.Delete,
        title = "清理错误导入数据",
        subtitle = "删除今天通过导入/手动添加的误操作数据",
        onClick = { showCleanupDialog = true }
    )
}
```

新增状态变量：
```kotlin
var showCleanupDialog by remember { mutableStateOf(false) }
```

新增确认对话框（放在 `importResult?.let` 同级位置）：
```kotlin
if (showCleanupDialog) {
    AlertDialog(
        onDismissRequest = { showCleanupDialog = false },
        title = { Text("清理错误导入数据") },
        text = {
            Text(
                "将删除今天通过CSV/TXT导入或手动添加的记录（通知自动记账的数据不受影响）。\n\n此操作不可撤销，确认继续？"
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    showCleanupDialog = false
                    scope.launch {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startOfToday = calendar.timeInMillis
                        val endOfToday = startOfToday + 24 * 60 * 60 * 1000 - 1
                        val deleted = withContext(Dispatchers.IO) {
                            expenseRepository.deleteImportedDataOnDay(startOfToday, endOfToday)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "已清理 $deleted 条错误导入记录，通知自动记账数据未受影响",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { showCleanupDialog = false }) {
                Text("取消")
            }
        }
    )
}
```

需要的新增 import：
```kotlin
import java.util.Calendar
```
（注：CurrentTimeMillis 已在 SettingsScreen 中使用，无需额外 import `Toast` 已有 L5）

还需要注入 ExpenseRepository。当前 settingsScreen 没有注入。需要：
```kotlin
val expenseRepository = runBlocking {
    EntryPoints.get(context.applicationContext, ExpenseRepoEntryPoint::class.java).expenseRepository()
}
```

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpenseRepoEntryPoint {
    fun expenseRepository(): ExpenseRepository
}
```

### Step 4：ImportManager.kt — 修复导入时间解析

**文件**：`app/src/main/java/com/example/autobookkeeper/ui/importdata/ImportManager.kt`

新增 `parseDateTime()` 方法：
```kotlin
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

private fun parseDateTime(dateStr: String): Long {
    val cleaned = dateStr.trim().replace("\"", "")
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == "日期时间") {
        Log.w("ImportManager", "空日期字段，使用当前时间")
        return System.currentTimeMillis()
    }
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm",
        "MM-dd HH:mm",
        "yyyy年MM月dd日 HH:mm"
    )
    formats.forEach { pattern ->
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.isLenient = false
            return sdf.parse(cleaned)?.time ?: return@runCatching
        }
    }
    Log.w("ImportManager", "无法解析日期: '$dateStr'，使用当前时间")
    return System.currentTimeMillis()
}
```

修改 CSV 导入（L80）：
```kotlin
// 修改前
recordedAt = System.currentTimeMillis(),
// 修改后
recordedAt = parseDateTime(cols[0]),
```

修改 TXT 导入（L119）：
```kotlin
// 修改前
recordedAt = System.currentTimeMillis(),
// 修改后
recordedAt = parseDateTime(map["日期时间"] ?: map["日期"] ?: ""),
```

需要的新增 import：
```kotlin
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
```

### Step 5：RecordsScreen.kt — 添加日期时间选择器

**文件**：`app/src/main/java/com/example/autobookkeeper/ui/screen/RecordsScreen.kt`

在 `AddExpenseSheet` 函数中（"支付渠道（可选）"输入框之后，"理财支出标记" Switch 之前）新增日期时间选择行：

```kotlin
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// 在 AddExpenseSheet 参数列表后新增状态变量：
var selectedDate by remember { mutableStateOf(LocalDate.now()) }
var selectedTime by remember { mutableStateOf(LocalTime.now()) }
var showDatePicker by remember { mutableStateOf(false) }
var showTimePicker by remember { mutableStateOf(false) }
```

在支付渠道输入框和理财支出标记之间插入 UI：

```kotlin
Spacer(modifier = Modifier.height(16.dp))

Text(
    text = "日期时间",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(modifier = Modifier.height(4.dp))

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
            fontSize = 14.sp
        )
    }
    OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            Icons.Default.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute),
            fontSize = 14.sp
        )
    }
}
```

DatePicker 弹窗（放在理财支出开关之前，日期时间选择行之后）：
```kotlin
if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                showDatePicker = false
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
```

TimePicker 弹窗：
```kotlin
if (showTimePicker) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
            TextButton(onClick = {
                selectedTime = LocalTime.of(
                    timePickerState.hour, timePickerState.minute
                )
                showTimePicker = false
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { showTimePicker = false }) {
                Text("取消")
            }
        },
        text = { TimePicker(state = timePickerState) }
    )
}
```

修改保存按钮中的 `recordedAt`（L543-551）：
```kotlin
// 修改前
val now = System.currentTimeMillis()
val record = ExpenseRecord(
    ...
    recordedAt = now,
    notificationId = "manual_$now"
)

// 修改后
val recordedAt = selectedDate
    .atTime(selectedTime)
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
val record = ExpenseRecord(
    ...
    recordedAt = recordedAt,
    notificationId = "manual_$recordedAt"
)
```

---

## 验证步骤

1. **安全删除**：点击"清理"按钮 → 对话框确认 → Toast 显示删除数量 → 自动记账数据未受影响
2. **导入时间**：导入示例 CSV（日期列为历史日期） → 记录显示在对应历史日期而非今天
3. **日期选择器**：手动添加 → 选择历史日期 + 时间 → 保存后在规定日期可见
4. **回归测试**：现有通知自动记账功能不受任何影响（notificationId 格式不匹配删除条件）
