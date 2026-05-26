# Tasks

- [x] Task 1: 修复时间格式化崩溃
  - [x] 将 RecordsScreen.kt 第 636 行 `LocalLocale.current.platformLocale` 替换为 `Locale.getDefault()`
  - [x] 移除第 80 行未使用的 `import androidx.compose.ui.platform.LocalLocale`

# Task Dependencies
- 无依赖，单行修复