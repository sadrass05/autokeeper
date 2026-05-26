# SettingsScreen 数据同步 → ModalBottomSheet 重构

## 目标
将 SettingsScreen 中内嵌展开的"数据同步"区块改为简洁入口行 + ModalBottomSheet 弹窗。

## 当前状态
- [SettingsScreen.kt:193-447](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/ui/screen/SettingsScreen.kt#L193-L447)：`Card` 内嵌 IP/端口输入、测试连接、详细诊断、全量/增量同步按钮
- 状态变量在 L174-184，已在 `SettingsScreen()` 作用域，无需迁移
- 现有 `SettingsRow` 组件（L738-785）可复用入口行样式

## 决策
| 决策 | 选择 |
|------|------|
| 呈现方式 | **ModalBottomSheet**（用户指定，优于 AlertDialog — 更符合 Material3 设计语言） |
| 入口行 | 复用 `SettingsRow` 风格（与其他设置项一致） |

---

## 变更范围

### 仅修改 1 个文件
`app/src/main/java/com/example/autobookkeeper/ui/screen/SettingsScreen.kt`

---

## 具体变更

### Step 1：新增 import

```kotlin
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
```

注：`navigationBarsPadding` 需要新增 `navigationBarsPadding` import（或作为 `WindowInsets` 扩展）。当前已有 `navigationBars` 的 import（L18），需要补充 `navigationBarsPadding`：

```kotlin
import androidx.compose.foundation.layout.navigationBarsPadding
```

### Step 2：替换同步 Card（L193–447）为入口行

**替换前**（约 255 行，L193–447）：
```kotlin
Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
) {
    Column(...) {
        // IP/端口 / 测试连接 / 诊断 / 全量同步 / 增量同步
    }
}
```

**替换后**（约 15 行）：
```kotlin
GlassCard(contentPadding = PaddingValues(0.dp)) {
    SettingsRow(
        icon = Icons.Default.Refresh,
        title = "数据同步",
        subtitle = if (syncPrefs.lastSyncTime > 0L)
            "上次同步: $lastSyncFormatted"
        else
            "点击配置同步服务器",
        onClick = { showSyncSheet = true }
    )
}
```

入口行后面的 `Spacer(modifier = Modifier.height(12.dp))` 保持不变。

### Step 3：新增 `showSyncSheet` 状态变量

在现有状态变量声明区域（约 L182 附近）添加：
```kotlin
var showSyncSheet by remember { mutableStateOf(false) }
```

### Step 4：新增 ModalBottomSheet（放在 Scaffold 闭合后、`importResult?.let` 前）

在 `Scaffold` 的 `}` 闭合后（L686）和 `importResult?.let`（L688）之间插入 BottomSheet：

```kotlin
if (showSyncSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { showSyncSheet = false },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "数据同步配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "上次同步: $lastSyncFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "服务器设置",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = {
                        serverIp = it
                        syncPrefs.serverIp = it
                    },
                    label = { Text("IP地址") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = serverPortText,
                    onValueChange = {
                        serverPortText = it
                        val port = it.toIntOrNull()
                        if (port != null && port in 1..65535) {
                            syncPrefs.serverPort = port
                        }
                    },
                    label = { Text("端口") },
                    placeholder = { Text("5000") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    isPinging = true
                    pingMessage = ""
                    pingSuccess = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            syncService.ping(serverIp, syncPrefs.serverPort)
                        }
                        withContext(Dispatchers.Main) {
                            isPinging = false
                            if (result == "ok") {
                                pingMessage = "连接成功"
                                pingSuccess = true
                            } else {
                                pingMessage = "连接失败: $result"
                                pingSuccess = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverIp.isNotBlank() && !isPinging
            ) {
                if (isPinging) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("测试连接")
            }

            if (pingMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pingMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pingSuccess == true) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { /* 详细诊断逻辑同前 */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverIp.isNotBlank() && !isDiagnosing
            ) {
                if (isDiagnosing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("详细诊断")
            }

            if (diagnosticsSteps.isNotEmpty() && !isDiagnosing) {
                Spacer(modifier = Modifier.height(8.dp))
                diagnosticsSteps.forEach { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (step.success) "\u2705" else "\u274C",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = step.stepName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (step.success) Color(0xFF4CAF50)
                                        else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = step.detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { /* 全量同步逻辑同前 */ },
                    modifier = Modifier.weight(1f),
                    enabled = serverIp.isNotBlank() && !isSyncing && pingSuccess == true
                ) {
                    Text("全量同步")
                }
                OutlinedButton(
                    onClick = { /* 增量同步逻辑同前 */ },
                    modifier = Modifier.weight(1f),
                    enabled = serverIp.isNotBlank() && !isSyncing && pingSuccess == true
                ) {
                    Text("增量同步")
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (syncMessage.isNotBlank() && !isSyncing) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = syncMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (syncSuccess == true) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

### Step 5：完整代码结构（变更后）

```
SettingsScreen() {
    // 状态变量（同步相关保持原位，新增 showSyncSheet）
    var showSyncSheet by remember { mutableStateOf(false) }
    // ... 其他状态变量不变 ...

    Scaffold {
        Column {
            SectionHeader("通知")
            // 通知设置...

            SectionHeader("数据")

            // ★ 入口行（替换原来的 Card）
            SettingsRow("数据同步", "上次同步/点击配置", onClick = { showSyncSheet = true })

            // 导入/导出/回收站（不变）

            SectionHeader("外观")
            // 深色模式...

            SectionHeader("关于")
            // 版本...
        }
    }

    // ★ 同步 BottomSheet
    if (showSyncSheet) {
        ModalBottomSheet(...) {
            // 完整同步配置内容
        }
    }

    // 导入结果 AlertDialog（不变）
    importResult?.let { ... }
}
```

---

## 注意事项

1. **状态变量作用域不变**：`serverIp`, `pingMessage`, `isSyncing` 等已在 `SettingsScreen()` 顶层，BottomSheet 可直接访问，无需任何迁移
2. **入口行图标**：使用 `Icons.Default.Refresh`（与深色模式一致但语义不同，`Refresh` 图标在 Material 中代表同步/刷新）
3. **BottomSheet dismiss 时不清空状态**：用户关闭后重新打开，IP 和上次测试结果保留
4. **skipPartiallyExpanded = true**：BottomSheet 直接全展开，不显示半展开状态

---

## 验证步骤
1. 设置页只显示简洁的"数据同步"入口行，不展开同步控件
2. 点击入口行弹出 ModalBottomSheet，包含完整同步配置
3. BottomSheet 内所有功能正常：IP 输入、测试连接、详细诊断、全量/增量同步
4. 关闭 BottomSheet 后状态保留，再次打开仍可见之前的结果
5. 无编译错误，无未使用 import
