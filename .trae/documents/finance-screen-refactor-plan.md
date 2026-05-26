# 理财页面重构计划

## 问题分析

当前理财页面的三个问题：
1. "收益排行"展示的是理财产品收益率排行，应改为展示理财支出账单排行（来源：`expenses` 表 `isFinanceExpense=true`）
2. 顶部收益统计卡片将"总市值"和"累计收益"（系统计算的持仓总收益）混淆，"理财收益支出"和"剩余净收益"占用空间
3. 缺少用户手动填写的独立"累计收益"字段，"净收益"逻辑需要重构为：**净收益 = 累计收益（用户输入） + 持仓收益（系统计算）**

---

## 实施步骤

### Step 1: 新建 FinancePrefs（持久化累计收益）

**新建文件**: `data/FinancePrefs.kt`

参照 [SyncPrefs.kt](file:///a:/study/Trae%20CN/work/money/app/src/main/java/com/example/autobookkeeper/data/SyncPrefs.kt) 模式，使用 `@ApplicationContext` + `SharedPreferences` 存储用户输入的累计收益值。

```kotlin
@Singleton
class FinancePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE)

    var accumulatedProfit: Double
        get() = prefs.getFloat("accumulated_profit", 0f).toDouble()
        set(value) = prefs.edit().putFloat("accumulated_profit", value.toFloat()).apply()
}
```

---

### Step 2: ViewModel 层 — 新增状态 + 修改计算逻辑

**文件**: `ui/viewmodel/MainViewModel.kt`

#### 2a. 注入 FinancePrefs

在构造函数添加：`private val financePrefs: FinancePrefs`

#### 2b. 新增累计收益 StateFlow

```kotlin
private val _accumulatedProfit = MutableStateFlow(financePrefs.accumulatedProfit)
val accumulatedProfit: StateFlow<Double> = _accumulatedProfit.asStateFlow()
```

新增修改方法：
```kotlin
fun setAccumulatedProfit(value: Double) {
    financePrefs.accumulatedProfit = value
    _accumulatedProfit.value = value
}
```

#### 2c. 新增理财支出排行 StateFlow

从 `expenses` 表中筛选 `isFinanceExpense=true` 的记录，按金额降序排列：

```kotlin
private val _financeExpenseRecords = MutableStateFlow<List<ExpenseRecord>>(emptyList())
val financeExpenseRecords: StateFlow<List<ExpenseRecord>> = _financeExpenseRecords.asStateFlow()
```

在 `init` 块中加载：
```kotlin
viewModelScope.launch {
    expenseRepository.getFinanceFlaggedExpenses().collect {
        _financeExpenseRecords.value = it.sortedByDescending { e -> e.amount }
    }
}
```

#### 2d. 修改净收益计算逻辑

删除原来的 `calculateNetFinanceProfit()`：
```kotlin
// 旧逻辑（删除）
_netFinanceProfit.value = _totalProfit.value - _financeExpense.value
```

改为：
```kotlin
private fun calculateNetFinanceProfit() {
    viewModelScope.launch {
        _netFinanceProfit.value = _accumulatedProfit.value + _totalProfit.value
    }
}
```

#### 2e. 更新 init 块 collectors

- 删除 `financeExpense.collect { calculateNetFinanceProfit() }`（净收益不再依赖理财支出）
- 添加 `accumulatedProfit.collect { calculateNetFinanceProfit() }`（累计收益变化时重算）
- 保留 `totalProfit.collect { calculateNetFinanceProfit() }`（持仓收益变化时重算）

---

### Step 3: FinanceScreen — 重构顶部概览卡片

**文件**: `ui/screen/FinanceScreen.kt`

#### 3a. 新增状态变量

```kotlin
val accumulatedProfit by viewModel.accumulatedProfit.collectAsStateWithLifecycle()
val financeExpenseRecords by viewModel.financeExpenseRecords.collectAsStateWithLifecycle()
var showEditAccumulatedProfit by remember { mutableStateOf(false) }
var accumulatedProfitEditText by remember { mutableStateOf("") }
```

#### 3b. 替换概览卡片（L185-268）

原布局：2×2 网格（总市值 | 累计收益 / 理财收益支出 | 剩余净收益）+ 底部净收益条
新布局：1×2 单行（持仓收益 | 累计收益 [可点击编辑]）+ 底部净收益条

- "持仓收益"：系统计算 `totalProfit`，绿色/红色不变
- "累计收益"：用户输入 `accumulatedProfit`，显示编辑图标，点击弹出 AlertDialog
- 底部条：`净收益 = 累计收益 + 持仓收益`

#### 3c. 新增累计收益编辑对话框

在 FinanceScreen 末尾（L754 前）添加：
```kotlin
if (showEditAccumulatedProfit) {
    AlertDialog(
        onDismissRequest = { showEditAccumulatedProfit = false },
        title = { Text("设置累计收益") },
        text = {
            OutlinedTextField(
                value = accumulatedProfitEditText,
                onValueChange = { accumulatedProfitEditText = it },
                label = { Text("累计收益金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val value = accumulatedProfitEditText.toDoubleOrNull() ?: return@Button
                viewModel.setAccumulatedProfit(value)
                showEditAccumulatedProfit = false
            }) { Text("确定") }
        },
        dismissButton = { Button(onClick = { showEditAccumulatedProfit = false }) { Text("取消") } }
    )
}
```

---

### Step 4: FinanceScreen — 重构"收益排行"为"理财支出排行"

**文件**: `ui/screen/FinanceScreen.kt`（L378-481）

#### 4a. 标题修改
`"收益排行"` → `"理财支出排行"`

#### 4b. 内容替换
- 原内容：`positions` 按 profit/profitRate 排序 + 排序切换按钮
- 新内容：`financeExpenseRecords`（已按 amount 降序排列）+ RankBadge + 商户名/金额/日期

#### 4c. 移除的代码
- `rankByProfit` 状态变量
- 排序切换 `FilledTonalButton` 两兄弟
- `rankedPositions` 排序逻辑
- `position.profitRate` 显示

#### 4d. 新列表样式
与"收益支出记录"区块保持一致的 GlassCard + 每条显示：
- RankBadge（排名数字 1-10）
- 商户名（加粗）+ 平台·支付渠道（灰色小字）
- -¥金额（红色加粗）+ 日期

---

## 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/FinancePrefs.kt` | **新建** | SharedPreferences 持久化累计收益 |
| `ui/viewmodel/MainViewModel.kt` | 修改 | 注入 FinancePrefs；新增 `accumulatedProfit` / `financeExpenseRecords` StateFlow；修改 `calculateNetFinanceProfit()` 逻辑；新增 `setAccumulatedProfit()`；调整 init collectors |
| `ui/screen/FinanceScreen.kt` | 修改 | 概览卡片 2×2→2×1（累计收益可点击编辑）；收益排行→理财支出排行（内容改为 ExpenseRecord）；新增累计收益编辑 AlertDialog |

## 数据流变更

```
持仓表 profit SUM ────────→ _totalProfit (系统计算)
SharedPreferences ────────→ _accumulatedProfit (用户手动输入)
                                          │
                            ┌─────────────┘
                            ▼
              _netFinanceProfit = accumulatedProfit + totalProfit
                            │
                            ▼
                      概览卡片 "净收益"

expenses 表 isFinanceExpense=true ──→ _financeExpenseRecords (按金额降序)
                                               │
                                               ▼
                                       "理财支出排行" 列表
```