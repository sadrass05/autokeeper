# UI 修复 + 通知解析增强 + 数据导入 — 实施计划

---

## 问题 1：通知解析失败 — 根因分析与修复

### 根因
| # | 问题 | 位置 |
|---|------|------|
| 1 | 只读 `EXTRA_TEXT`，漏掉 `EXTRA_BIG_TEXT` / `EXTRA_SUMMARY_TEXT` | NotificationListener.kt:L44 |
| 2 | 微信通知标题可能是商户名而非"微信支付"，`isWechatPayment` 直接返回 false | PaymentParser.kt:L36 |
| 3 | 正则只匹配 `向XX支付X元`，不覆盖 `支付成功 ¥X` / `付款金额：¥X` 等真实格式 | PaymentParser.kt:L48-100 |

### 修复方案

#### Step 1.1: 增强通知内容提取（NotificationListener.kt）
- 修改 `getText()` 方法，同时提取 `EXTRA_TEXT` + `EXTRA_BIG_TEXT` + `EXTRA_SUMMARY_TEXT`
- 合并所有文本用于匹配
- 添加 `android.util.Log` 日志打印每个通知的 packageName / title / 全部文本

#### Step 1.2: 重写 PaymentParser 为全量匹配
**微信支付通知格式覆盖**：
```
"微信支付" → 支付成功 ¥xx.xx
"微信支付" → 付款金额 ¥xx.xx  
"微信支付" → 向XX支付xx元
"微信支付" → 消费xx元
"微信支付" → xx商户 支付xx元
"微信支付" → 支付xx.xx元
"微信支付" → 已支付¥xx.xx
"微信支付" → 扣费xx元
```

**支付宝通知格式覆盖**：
```
"支付宝" → 成功付款xx.xx元
"支付宝" → 付款成功-xx元
"支付宝" → 消费xx元
"支付宝" → 交易提醒：支出xx元
"支付宝" → 向XX付款xx元
"支付宝" → 扣款xx元
"支付宝" → 支付xx元
```

**拼多多通知格式覆盖**：
```
"拼多多" → 支付成功 xx元
"拼多多" → 已支付¥xx
"拼多多" → 订单支付成功 xx元
"拼多多" → 待发货-已付xx元
"拼多多" → 付款成功 xx元
```

**匹配策略**：不再依赖"微信支付"精确关键词，改为：
- 微信包名 `com.tencent.mm` → 检查是否包含金额（`¥` 或 `元`）+ 支付相关关键词（支付/付款/消费/扣费/扣款）
- 支付宝包名 `com.eg.android.AlipayGphone` → 同上
- 拼多多包名 `com.xunmeng.pinduoduo` → 同上

**金额正则**：统一使用 `¥\s*([0-9]*\.?[0-9]+)|([0-9]*\.?[0-9]+)\s*元`

**商户名提取**：多模式尝试
1. `向(.+?)(支付|付款|消费)` 
2. `(.+?)\s*(支付|付款|消费)\s*[¥¥]`
3. 无商户名时取通知标题

---

## 问题 2：首页 UI 修复

### 当前问题
- 显示"本月净收支"和 SummaryRing（支出 vs 收益环形图）
- 用户要求改为"当日支出"+"本月支出"，去除净收支

### 修复方案

#### Step 2.1: MainViewModel 新增当日支出字段
```kotlin
private val _dailyExpense = MutableStateFlow(0.0)
val dailyExpense: StateFlow<Double> = _dailyExpense.asStateFlow()

private fun calculateDailyExpense() {
    viewModelScope.launch {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis
        _dailyExpense.value = expenseRepository.getTotalExpenseByDay(startOfDay, endOfDay)
    }
}
```

#### Step 2.2: ExpenseDao 新增当日查询
```kotlin
@Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = false AND recordedAt >= :startTime AND recordedAt <= :endTime")
suspend fun getTotalExpenseByDay(startTime: Long, endTime: Long): Double?
```

#### Step 2.3: 重写 HomeScreen.kt Hero 区域
```
┌──────────────────────────────┐
│      自动记账助手              │
│                              │
│   今日支出                    │
│   ¥128.50                   │  ← displayLarge, NegativeRed
│                              │
│   本月支出  ¥2,860.00        │  ← titleLarge
│   理财收益  ¥320.00          │  ← titleLarge, PositiveGreen
│                              │
│   ─── 支出趋势 ───           │
│   [MiniLineChart]            │
│                              │
│   最近交易                    │
│   ...                        │
└──────────────────────────────┘
```
- 移除 `SummaryRing`
- 移除 `netProfit` 和 `LaunchedEffect`
- 顶部大数字：今日支出（红色强调）
- 下方两行：本月支出 + 理财收益

---

## 问题 3：数据导入功能

### 需求
- 支持导入 txt 和 Excel 格式
- 导入的支出记录写入本地 Room 数据库

### 支持的文件格式

#### TXT 格式
```
2024-01-15 12:30  餐饮  微信  银行卡  35.50  午餐
2024-01-15 18:00  交通  支付宝  余额  12.00  地铁
```
每行：日期 时间 分类 平台 支付渠道 金额 商户名（制表符或逗号分隔）

#### Excel 格式
| 日期 | 时间 | 分类 | 平台 | 支付渠道 | 金额 | 商户 | 是否理财支出 |
|------|------|------|------|---------|------|------|------------|
| 2024-01-15 | 12:30 | 餐饮 | 微信 | 银行卡 | 35.50 | 午餐 | 否 |

### 修复方案

#### Step 3.1: 新建 ImportManager.kt
```kotlin
// ui/import/ImportManager.kt
class ImportManager @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend fun importTxt(content: String): ImportResult
    suspend fun importExcel(filePath: String): ImportResult
}
```

#### Step 3.2: TXT 解析
- 按行分割
- 每行用 tab 或逗号分割
- 解析字段：日期、时间、分类、平台、渠道、金额、商户
- 生成 ExpenseRecord 批量插入

#### Step 3.3: Excel 解析（Apache POI）
- 使用项目已有的 Apache POI 依赖
- 读取第一个 Sheet
- 跳过表头行
- 逐行解析并插入

#### Step 3.4: UI 入口
- 设置页新增"导入数据"行
- 点击后打开文件选择器（支持 .txt 和 .xlsx）
- 导入完成后显示结果（成功 N 条，跳过 M 条）

#### Step 3.5: AndroidManifest 添加文件读取权限（如需要）

---

## 实施步骤（按依赖排序）

### Step 1: 通知解析修复
- 修改 `NotificationListener.kt`：增强文本提取 + 日志
- 重写 `PaymentParser.kt`：全量格式覆盖

### Step 2: 首页 UI 修复
- 修改 `ExpenseDao.kt`：新增 `getTotalExpenseByDay`
- 修改 `MainViewModel.kt`：新增 `dailyExpense` 字段
- 重写 `HomeScreen.kt`：今日支出 + 本月支出布局

### Step 3: 数据导入功能
- 新建 `ui/import/ImportManager.kt`
- 新建 `ui/import/ImportResult.kt`
- 修改 `SettingsScreen.kt`：新增导入入口
- 新建文件选择逻辑

---

## 保护的文件（不修改）
- `build.gradle` / `app/build.gradle`
- `AndroidManifest.xml`
- `di/` 所有文件
- `data/entity/` 所有实体类
- `data/AppDatabase.kt`
- `ocr/`、`excel/`、`network/` 所有文件
- `ui/theme/` 所有文件
- `ui/components/` 图表组件
- `RecordsScreen.kt`、`FinanceScreen.kt`
- `MainActivity.kt`
- `res/` 所有资源文件