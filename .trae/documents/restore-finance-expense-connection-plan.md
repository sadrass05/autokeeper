# 理财支出记录连接恢复与视觉标记计划

## 问题分析

当前状态：
1. `expenses` 表中存在 `isFinanceExpense=true` 的历史记录（通过手动添加的 Switch 开关标记）
2. `FinanceScreen` 的"收益支出记录"区块只展示新表 `finance_expenses` 的记录
3. `FinanceScreen` 概览卡片的"理财收益支出"总额只汇总新表 `SUM(amount)`
4. `HomeScreen` 的 `TransactionItem` 和 `RecordsScreen` 的 `SwipeableRecordItem` 中，`isFinanceExpense=true` 的记录没有任何视觉区分

用户需求：
- 将旧的 `isFinanceExpense=true` 记录纳入理财支出记录列表和总额计算
- 在首页和支出记录页面中为理财支出记录添加特殊视觉标记

---

## 实施步骤

### Step 1: Repository 层 — 暴露旧表理财支出数据

**文件**: `data/repository/ExpenseRepository.kt`

在 `ExpenseRepository` 中新增两个方法：

```kotlin
fun getFinanceFlaggedExpenses(): Flow<List<ExpenseRecord>> {
    return expenseDao.getExpensesByFinanceFlag(true)
}

suspend fun getTotalFinanceFlaggedExpense(): Double {
    return expenseDao.getTotalFinanceExpense() ?: 0.0
}
```

> 注意：DAO 中已有 `getExpensesByFinanceFlag(Boolean)` 和 `getTotalFinanceExpense()`，无需新增 DAO 方法。

---

### Step 2: ViewModel 层 — 合并新旧数据源

**文件**: `ui/viewmodel/MainViewModel.kt`

#### 2a. 修改收益支出总额计算

`calculateFinanceExpense()` 需要同时汇总新旧两表：

```kotlin
private fun calculateFinanceExpense() {
    viewModelScope.launch {
        val newTableTotal = financeExpenseRepository.getTotalExpenses()
        val oldTableTotal = expenseRepository.getTotalFinanceFlaggedExpense()
        _financeExpense.value = newTableTotal + oldTableTotal
    }
}
```

#### 2b. 合并收益支出列表

新建一个合并方法，将旧表 `ExpenseRecord`（isFinanceExpense=true）转换为 `FinanceExpense` 对象并与新表数据合并：

```kotlin
private fun loadFinanceExpenses() {
    viewModelScope.launch {
        combine(
            financeExpenseRepository.getAllExpenses(),
            expenseRepository.getFinanceFlaggedExpenses()
        ) { newRecords, oldRecords ->
            val converted = oldRecords.map { record ->
                FinanceExpense(
                    amount = record.amount,
                    description = record.merchant.ifEmpty { record.category },
                    fromProduct = record.platform,
                    recordedAt = record.recordedAt
                )
            }
            (newRecords + converted).sortedByDescending { it.recordedAt }
        }.collect { _financeExpenses.value = it }
    }
}
```

> 注意：`id` 字段使用默认值 0（autoGenerate），这些记录不会被写入数据库，仅用于 UI 展示。

---

### Step 3: FinanceScreen — 隐藏旧记录删除按钮

**文件**: `ui/screen/FinanceScreen.kt`

旧表转换来的 `FinanceExpense` 记录 `id == 0`（默认值），新表的记录 `id > 0`（自增主键）。因此：

- `id == 0` 的记录：**不显示删除按钮**（需在原支出记录中删除）
- `id > 0` 的记录：正常显示删除按钮

修改收益支出记录列表中每条记录的渲染逻辑，对 `id == 0` 的记录移除 `IconButton` 删除按钮。

---

### Step 4: RecordsScreen — 为理财支出记录添加视觉标记

**文件**: `ui/screen/RecordsScreen.kt`

在 `SwipeableRecordItem` 组件中，为 `isFinanceExpense=true` 的记录添加视觉标记：

#### 4a. 金额颜色改变
- 理财支出金额使用 **琥珀金色** `#FFB300`（与普通支出的红色不同）
- 非理财支出保持原有红色

#### 4b. 特殊图标
- 左侧 `CategoryCircleIcon` 对于理财支出记录，图标替换为金币/盾牌符号
- 可使用 `Icons.Default.Savings` 或自定义图标
- 圆形背景色变为琥珀金色浅色背景（`#FFF8E1`）

#### 4c. "理财"标记标签
- 在平台/时间信息行末尾添加一个小矩形标签文字 `理财`，使用金色

具体改动点（`SwipeableRecordItem` 函数，约 L694-773）：

1. **入口**：添加 `import androidx.compose.material.icons.filled.Star`（或 Savings）
2. **金额颜色**（L764-771）：理财支出 → `Color(0xFFFFB300)`（amber gold），非理财 → 保持原逻辑
3. **图标**（L743）：理财支出 → `CategoryCircleIcon` 替换为自定义金色圆+星形图标
4. **标签**（L752-762）：理财支出 → 在时间行追加带背景的 "理财" Text

---

### Step 5: HomeScreen — 为理财支出记录添加视觉标记

**文件**: `ui/screen/HomeScreen.kt`

在 `TransactionItem` 组件中，为 `isFinanceExpense=true` 的记录添加视觉标记：

#### 5a. 金额颜色改变
- 理财支出金额使用 **琥珀金色** `#FFB300`
- 非理财支出保持原有红色

#### 5b. 左侧分类图标
- 理财支出记录的圆形图标背景由 `surfaceVariant` 改为琥珀金浅色（`#FFF8E1`）
- 图标文字内容改为 `"💰"` emoji（或保留原分类首字母但改变颜色）

#### 5c. 妙计标签
- 在 merchants/平台行添加带金色背景的小标签文字 `理财`

具体改动点（`TransactionItem` 函数，L312-389）：

1. **金额颜色**（L317-321）：理财支出 → `Color(0xFFFFB300)`，非理财 → 保持原逻辑
2. **图标区背景**（L347-349）：理财支出 → 背景色 `Color(0xFFFFF8E1)`，文字色 `Color(0xFFFFB300)`
3. **标签**（L367-381）：理财支出 → 在平台行末尾追加小标签

---

## 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/repository/ExpenseRepository.kt` | 修改 | 新增 `getFinanceFlaggedExpenses()` 和 `getTotalFinanceFlaggedExpense()` |
| `ui/viewmodel/MainViewModel.kt` | 修改 | 合并新旧数据源到 `_financeExpenses`，`calculateFinanceExpense` 汇总双表（需添加 `combine` import） |
| `ui/screen/FinanceScreen.kt` | 修改 | `id==0` 的记录隐藏删除按钮 |
| `ui/screen/RecordsScreen.kt` | 修改 | `SwipeableRecordItem` 添加理财标记（图标+颜色+标签） |
| `ui/screen/HomeScreen.kt` | 修改 | `TransactionItem` 添加理财标记（图标+颜色+标签） |