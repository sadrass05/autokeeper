# 修复通知监听 - 任务列表

- [x] Task 1: 修复 AndroidManifest.xml 中 Service 类名
  - 将 `android:name=".notification.NotificationListenerService"` 改为 `android:name=".notification.NotificationListener"`
  - 验证：构建项目不报错

- [x] Task 2: 修复 PaymentParser.parsePayment 函数签名
  - 修改 `parsePayment` 函数签名为 `fun parsePayment(packageName: String, title: String, text: String, notificationId: String): ExpenseRecord?`
  - 确保调用处 `NotificationListener.kt` 的参数顺序与签名一致
  - 验证：编译通过

# Task Dependencies
- Task 2 与 Task 1 无依赖关系，可并行执行