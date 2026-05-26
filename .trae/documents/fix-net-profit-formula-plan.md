# 理财净收益公式修正计划

## 问题
当前净收益公式为 `净收益 = 累计收益 + 持仓收益`，用户明确要求改为 `净收益 = 累计收益 - 总收益支出`。

## 当前状态

| 变量 | 含义 | 来源 |
|------|------|------|
| `_accumulatedProfit` | 用户手动输入的累计收益 | `FinancePrefs` (SharedPreferences) |
| `_totalProfit` | 系统计算的持仓总收益 | `financeDao.getTotalProfit()` (所有持仓 profit 字段 SUM) |
| `_financeExpense` | 总收益支出 | 新表 `finance_expenses` SUM + 旧表 `isFinanceExpense=true` SUM |
| `_netFinanceProfit` | 净收益 | 当前：`accumulatedProfit + totalProfit` |

## 目标公式

```
净收益 = 累计收益(用户输入) - 总收益支出(所有理财支出之和)
```

---

## 实施步骤

### Step 1: 修改净收益计算公式

**文件**: `ui/viewmodel/MainViewModel.kt` L240-243

```kotlin
// 旧
_netFinanceProfit.value = _accumulatedProfit.value + _totalProfit.value

// 新
_netFinanceProfit.value = _accumulatedProfit.value - _financeExpense.value
```

### Step 2: 调整 init 块 collectors

**文件**: `ui/viewmodel/MainViewModel.kt` L139-145

- 删除 `totalProfit.collect { calculateNetFinanceProfit() }`（净收益不再依赖持仓收益）
- 添加 `financeExpense.collect { calculateNetFinanceProfit() }`（净收益依赖总收益支出）

```kotlin
// 旧
viewModelScope.launch {
    totalProfit.collect { calculateNetFinanceProfit() }
}

// 新
viewModelScope.launch {
    financeExpense.collect { calculateNetFinanceProfit() }
}
```

保留 `accumulatedProfit.collect { calculateNetFinanceProfit() }`。

### Step 3: 修正 FinanceScreen 底部文字

**文件**: `ui/screen/FinanceScreen.kt` L256

```kotlin
// 旧
text = "净收益 = 累计收益 + 持仓收益 = ${formatFinanceAmount(netFinanceProfit)}"

// 新
text = "净收益 = 累计收益 - 总收益支出 = ${formatFinanceAmount(netFinanceProfit)}"
```

---

## 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/viewmodel/MainViewModel.kt` | 修改 2 处 | 公式改为减法；`totalProfit.collect`→`financeExpense.collect` |
| `ui/screen/FinanceScreen.kt` | 修改 1 处 | 底部文字从 "+ 持仓收益" 改为 "- 总收益支出" |

## 数据流

```
_accumulatedProfit (用户输入)  ──┐
                                  ├── _netFinanceProfit = accumulatedProfit - financeExpense
_financeExpense (总支出)      ──┘
```