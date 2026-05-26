# 修复通知监听无法自动记账 - 规范文档

## Why
App已成功安装到手机并授予通知权限，但无法自动读取支付通知并记录支出。经代码审查发现两个关键Bug导致通知监听完全失效。

## What Changes
- 修复 AndroidManifest.xml 中 Service 类名与 Kotlin 类名不匹配的问题
- 修复 PaymentParser.parsePayment 函数签名与调用处参数不匹配的问题
- 不修改任何配置相关文件（build.gradle、NetworkModule、数据库配置等）

## Impact
- Affected specs: 自动记账核心功能
- Affected code:
  - `AndroidManifest.xml` - Service 声明
  - `PaymentParser.kt` - parsePayment 函数签名
  - `NotificationListener.kt` - 调用处参数

## Bug 分析

### Bug 1: Service 类名不匹配（致命）
- **AndroidManifest.xml:L24** 声明: `android:name=".notification.NotificationListenerService"`
- **NotificationListener.kt:L15** 实际类名: `class NotificationListener`
- **后果**: 系统无法找到并启动 Service，通知监听完全无效

### Bug 2: parsePayment 参数错位（致命）
- **NotificationListener.kt:L47** 调用: `paymentParser.parsePayment(packageName, title, text, notificationId)` — 4个参数
- **PaymentParser.kt:L22-27** 签名: `fun parsePayment(packageName, text, notificationId, notificationId1)` — 4个参数但语义错位
- **后果**: `title` 被当作 `text` 传入，`text` 被当作 `notificationId` 传入，导致解析出的商户名和金额都是通知标题而非正文

## ADDED Requirements

### Requirement: 通知监听 Service 正确注册
系统 SHALL 在 AndroidManifest.xml 中声明的 Service 类名与 Kotlin 实际类名一致。

#### Scenario: Service 被系统正确启动
- **WHEN** 用户在系统设置中授予通知权限
- **THEN** 系统应能成功绑定并启动 NotificationListenerService

### Requirement: 支付通知正确解析
系统 SHALL 正确地将通知标题和正文传递给 PaymentParser，确保金额和商户名从通知正文（EXTRA_TEXT）中提取。

#### Scenario: 微信支付通知被正确解析
- **WHEN** 收到微信支付通知，正文为"微信支付: 你向XX商户支付10.00元"
- **THEN** 系统应解析出 amount=10.00, merchant="XX商户", platform="微信"

## 不修改的文件（保护）
- `app/build.gradle`
- `build.gradle`
- `di/NetworkModule.kt`
- `di/DatabaseModule.kt`
- 所有 res/ 资源文件
- 所有 UI 文件
- 所有 data/ 数据层文件