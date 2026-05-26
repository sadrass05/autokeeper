# 修复数据同步和清理错误导入数据无效 Spec

## Why
SettingsScreen 中数据同步 ModalBottomSheet 和清理错误导入数据 AlertDialog 均无法弹出，用户点击对应入口后无任何反应。根因是状态变量被重复声明导致 Kotlin 变量遮蔽（shadowing），内层变量被修改但外层条件判断读取的是未修改的外层变量。

## What Changes
- **修改** `ui/screen/SettingsScreen.kt` — 移除 Column lambda 内重复声明的状态变量，统一使用外层声明

## Impact
- Affected specs: `local-network-sync`（数据同步功能恢复可用）
- Affected code: `ui/screen/SettingsScreen.kt`

## MODIFIED Requirements

### Requirement: 数据同步设置入口
系统 SHALL 在设置页面显示"数据同步"入口行，点击后弹出 ModalBottomSheet。

#### Scenario: 点击数据同步入口
- **WHEN** 用户在设置页点击"数据同步"行
- **THEN** ModalBottomSheet 弹出，显示服务器 IP/端口配置、测试连接、全量同步、增量同步等控件

### Requirement: 清理错误导入数据
系统 SHALL 在设置页面提供"清理错误导入数据"入口，点击后弹出确认对话框。

#### Scenario: 点击清理入口
- **WHEN** 用户在设置页点击"清理错误导入数据"行
- **THEN** AlertDialog 弹出，显示操作说明和确认/取消按钮

### Requirement: 状态变量作用域一致
系统 SHALL 确保每个 UI 状态变量在 Composable 函数内只声明一次，避免嵌套作用域中的变量遮蔽。