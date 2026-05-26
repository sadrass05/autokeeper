# 自动记账助手 - 设计文档

## 1. 项目概述

### 1.1 项目名称
自动记账助手（AutoBookkeeper）

### 1.2 核心目标
通过读取支付通知自动记录支出，统一管理微信/支付宝/拼多多等多平台消费数据，并支持理财分析。

### 1.3 用户需求分析

| 需求点 | 描述 |
|--------|------|
| 自动记账 | 读取微信、支付宝、拼多多等支付通知，自动记录支出 |
| 渠道识别 | 识别拼多多支付渠道（银行卡直付 vs 支付宝调用银行卡） |
| 数据同步 | 同步到用户已有MySQL数据库，支持电脑端Excel分析 |
| 理财分析 | 通过截图OCR识别理财持仓和收益，计算净收益 |
| 特殊标记 | 理财收益支出单独标记（记录但不计入总支出） |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                 │
│                                                         │
│  ┌──────────────────┐    ┌──────────────────────────┐  │
│  │ NotificationListener │ │   Screenshot OCR Module  │  │
│  │   Service          │   │   (Google ML Kit)        │  │
│  └────────┬─────────┘    └────────────┬─────────────┘  │
│           │                           │                 │
│           ▼                           ▼                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Data Processing Layer                │  │
│  └────────────────────────┬──────────────────────────┘  │
│                           │                            │
│                           ▼                            │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Local SQLite Database                │  │
│  └────────────────────────┬──────────────────────────┘  │
│                           │                            │
│  ┌───────────────────────────────────────────────────┐  │
│  │              UI Layer (Jetpack Compose)           │  │
│  └───────────────────────────────────────────────────┘  │
│                           │                            │
└───────────────────────────┼─────────────────────────────┘
                            │ 后台同步服务
                            ▼
                   ┌─────────────────┐
                   │  MySQL 服务器    │
                   └─────────────────┘
```

### 2.2 模块划分

| 模块 | 职责 |
|------|------|
| NotificationListener | 监听并解析支付App通知 |
| DataProcessing | 数据解析、渠道识别、收益计算 |
| LocalStorage | Room数据库操作 |
| UILayer | Jetpack Compose UI展示 |
| SyncService | 后台同步到MySQL |
| OCRModule | 截图文字识别 |
| ExcelExport | Excel文件导出 |

---

## 3. 核心功能设计

### 3.1 通知监听模块

**支持平台：**
- 微信支付
- 支付宝
- 拼多多

**解析规则：**
| 平台 | 通知模式 | 解析字段 |
|------|---------|---------|
| 微信支付 | "微信支付: 你向XX支付10.00元" | 金额、商户 |
| 支付宝 | "支付宝: 成功付款88.00元" | 金额、商户 |
| 拼多多 | "拼多多: 支付成功15.90元" | 金额、支付渠道 |

**拼多多渠道识别逻辑：**
- 检测通知中的"银行卡"、"支付宝"、"微信"关键词
- 若显示"支付宝"且后续有银行卡信息，标记为"支付宝调用银行卡"

### 3.2 支出记录模块

**数据模型：**
```kotlin
data class ExpenseRecord(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val platform: String,        // 微信/支付宝/拼多多
    val paymentChannel: String,  // 余额/银行卡/支付宝调用银行卡
    val category: String,        // 用户选择的分类
    val isFinanceExpense: Boolean, // 是否理财支出
    val recordedAt: Long,
    val notificationId: String   // 原始通知ID（防重复）
)
```

**处理流程：**
1. 收到支付通知 → 解析金额、商户、渠道
2. 弹出通知提醒用户确认
3. 用户点击选择分类
4. 标记是否为理财支出
5. 保存到本地数据库

### 3.3 理财分析模块

**数据模型：**
```kotlin
data class FinancePosition(
    val id: Long = 0,
    val productName: String,
    val platform: String,       // 支付宝理财/微信理财通/其他
    val buyAmount: Double,
    val currentValue: Double,
    val profit: Double,
    val profitRate: Double,
    val screenshotPath: String,
    val updatedAt: Long
)
```

**截图识别流程：**
1. 用户导入理财App截图
2. ML Kit OCR识别文字
3. 正则匹配提取持仓金额、收益金额、收益率
4. 用户确认/修正
5. 保存并计算净收益

**净收益计算公式：**
```
净收益 = 理财总收益 - 非理财总支出
```

### 3.4 统计报表模块

**功能列表：**
- 月度/年度支出统计
- 按分类统计
- 按平台统计
- 支出趋势图表
- 理财收益 vs 支出对比

### 3.5 导出Excel模块

**功能：**
- 导出指定时间范围的支出记录
- 导出理财持仓数据
- 生成统计汇总表
- 格式兼容Excel/WPS

### 3.6 数据同步模块

**同步策略：**
- 本地SQLite为主存储
- 后台服务定时同步到MySQL
- 支持手动触发同步
- 冲突处理：以最新修改时间为准

---

## 4. UI设计

### 4.1 页面结构

```
┌─────────────────────────────────┐
│  底部导航栏                      │
├─────────────────────────────────┤
│  📊 首页    📝 记录    💰 理财    ⚙️ 设置  │
├─────────────────────────────────┤
```

### 4.2 页面功能

| 页面 | 功能 |
|------|------|
| 首页 | 本月支出总览、最近交易列表、理财收益简报 |
| 记录 | 交易历史列表、筛选/搜索、导出Excel |
| 理财 | 持仓列表、导入截图、收益分析 |
| 设置 | 通知权限、分类管理、MySQL同步配置、导出设置 |

---

## 5. 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| 语言 | Kotlin | 2.0+ |
| UI框架 | Jetpack Compose | 1.6+ |
| 架构 | MVVM + Clean Architecture | - |
| 本地数据库 | Room | 2.6+ |
| 网络请求 | Retrofit + OkHttp | 2.9+ |
| OCR | Google ML Kit Text Recognition | 16.0+ |
| 图表 | MPAndroidChart | 3.1+ |
| Excel导出 | Apache POI | 5.2+ |
| 依赖注入 | Hilt | 2.5+ |
| 异步处理 | Kotlin Coroutines + Flow | - |

---

## 6. 数据库设计

### 6.1 支出记录表（expenses）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 |
| amount | DOUBLE | NOT NULL | 金额 |
| merchant | TEXT | NOT NULL | 商户名称 |
| platform | TEXT | NOT NULL | 支付平台 |
| payment_channel | TEXT | NOT NULL | 支付渠道 |
| category | TEXT | NOT NULL | 分类 |
| is_finance_expense | INTEGER | NOT NULL DEFAULT 0 | 是否理财支出 |
| recorded_at | INTEGER | NOT NULL | 记录时间戳 |
| notification_id | TEXT | UNIQUE | 原始通知ID |

### 6.2 理财持仓表（finance_positions）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 |
| product_name | TEXT | NOT NULL | 产品名称 |
| platform | TEXT | NOT NULL | 平台 |
| buy_amount | DOUBLE | NOT NULL | 买入金额 |
| current_value | DOUBLE | NOT NULL | 当前市值 |
| profit | DOUBLE | NOT NULL | 累计收益 |
| profit_rate | DOUBLE | NOT NULL | 收益率 |
| screenshot_path | TEXT | - | 截图路径 |
| updated_at | INTEGER | NOT NULL | 更新时间戳 |

### 6.3 分类配置表（categories）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 |
| name | TEXT | NOT NULL UNIQUE | 分类名称 |
| icon | TEXT | - | 图标名称 |
| color | INTEGER | - | 颜色值 |

---

## 7. API设计

### 7.1 后端同步API（MySQL）

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/expenses/sync | POST | 同步支出记录 |
| /api/expenses/list | GET | 查询支出列表 |
| /api/finance/sync | POST | 同步理财持仓 |
| /api/finance/list | GET | 查询持仓列表 |

### 7.2 请求/响应示例

**POST /api/expenses/sync**
```json
{
  "records": [
    {
      "id": 1,
      "amount": 100.0,
      "merchant": "美团外卖",
      "platform": "微信",
      "paymentChannel": "银行卡",
      "category": "餐饮",
      "isFinanceExpense": false,
      "recordedAt": 1714896000000,
      "notificationId": "notification_123"
    }
  ]
}
```

---

## 8. 安全与隐私

| 安全点 | 措施 |
|--------|------|
| 通知权限 | 用户手动授权，仅监听支付相关通知 |
| 数据加密 | 本地数据库加密，传输使用HTTPS |
| 隐私保护 | 不收集个人身份信息，仅存储必要的交易数据 |
| 权限最小化 | 仅申请必要的系统权限 |

---

## 9. 部署与集成

### 9.1 依赖配置

```gradle
// Room
implementation "androidx.room:room-runtime:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// ML Kit OCR
implementation "com.google.mlkit:text-recognition:16.0.0"

// Retrofit
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"

// MPAndroidChart
implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"

// Apache POI
implementation "org.apache.poi:poi:5.2.5"

// Hilt
implementation "com.google.dagger:hilt-android:2.51.1"
kapt "com.google.dagger:hilt-compiler:2.51.1"
```

---

## 10. 测试计划

| 测试类型 | 覆盖内容 |
|----------|---------|
| 单元测试 | 通知解析逻辑、数据处理层 |
| 集成测试 | 数据库操作、API同步 |
| UI测试 | 页面跳转、交互流程 |
| 功能测试 | 通知监听、截图识别、Excel导出 |

---

**文档版本：** v1.0  
**创建日期：** 2026-05-05  
**状态：** 待审核