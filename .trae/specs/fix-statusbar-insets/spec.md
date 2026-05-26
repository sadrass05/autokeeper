# 沉浸式状态栏适配修复 Spec

## Why
启用边到边绘制（edge-to-edge）后，4 个页面和 MainActivity 未正确处理 WindowInsets，导致页面内容从屏幕顶部 0px 开始绘制，与状态栏重叠。

## What Changes
- MainActivity.onCreate() 在 setContent 前调用 `enableEdgeToEdge()`
- HomeScreen / RecordsScreen / SettingsScreen 的 topBar 内 Text 添加 `.statusBarsPadding()`
- FinanceScreen（无 topBar）顶层 LazyColumn 添加 `.statusBarsPadding()`
- themes.xml 添加 `windowTranslucentStatus=false` 和 `windowTranslucentNavigation=false`

## Impact
- Affected specs: true-glass-navbar, refactor-homepage-chart
- Affected code: MainActivity.kt, HomeScreen.kt, RecordsScreen.kt, SettingsScreen.kt, FinanceScreen.kt, themes.xml
- New files: 无

---

## ADDED Requirements

### Requirement: 边到边绘制启用
系统 SHALL 在 MainActivity.onCreate() 中 setContent 前调用 enableEdgeToEdge()。

#### Scenario: 边到边启用
- **WHEN** Activity 创建
- **THEN** enableEdgeToEdge() 在 setContent 前执行
- **AND** 状态栏和导航栏背景设为透明

### Requirement: 页面顶部状态栏避让
系统 SHALL 在所有页面的顶层内容添加 statusBarsPadding() 避让状态栏。

#### Scenario: 有 topBar 的页面
- **WHEN** 渲染 HomeScreen / RecordsScreen / SettingsScreen 的 topBar
- **THEN** topBar 内标题 Text 使用 Modifier.statusBarsPadding().padding(horizontal, vertical)
- **AND** 标题文本显示在状态栏下方安全区域

#### Scenario: 无 topBar 的页面
- **WHEN** 渲染 FinanceScreen
- **THEN** 顶层 LazyColumn 在 contentPadding 后添加 .statusBarsPadding()
- **AND** 内容从状态栏下方安全区域开始绘制

### Requirement: 主题配置修复
系统 SHALL 在 themes.xml 中配置 windowTranslucentStatus 和 windowTranslucentNavigation 为 false。

#### Scenario: 主题配置
- **WHEN** App 启动
- **THEN** windowTranslucentStatus = false
- **AND** windowTranslucentNavigation = false
- **AND** 避免系统自动添加半透明遮罩