# Tasks

- [x] Task 1: 修复 SettingsScreen 重复状态变量导致的功能失效
  - 移除 SettingsScreen.kt 中 Column lambda 内（约 L200-212）重复声明的状态变量：`serverIp`、`serverPortText`、`isSyncing`、`syncMessage`、`syncSuccess`、`pingMessage`、`pingSuccess`、`isPinging`、`isDiagnosing`、`diagnosticsSteps`、`showSyncSheet`、`showCleanupDialog`、`lastSyncFormatted`
  - 保留 Column lambda 中仅与导出相关的局部状态变量（`isExportingExpenses`、`exportExpensesMessage` 等）
  - 验证：点击"数据同步"行能弹出 ModalBottomSheet，点击"清理错误导入数据"行能弹出 AlertDialog