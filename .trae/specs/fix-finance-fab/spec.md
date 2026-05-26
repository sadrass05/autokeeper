# FinanceScreen FAB 导航栏遮挡修复 Spec

## Why
FinanceScreen 的 FAB 被底部悬浮导航栏完全遮挡。导航栏作为 overlay 悬浮在 Scaffold 外，FAB 不知道其高度，紧贴屏幕底部 0px 位置。

## What Changes
- FAB modifier 添加 .padding(bottom = 80.dp).navigationBarsPadding()
- LazyColumn 末尾 spacer 从 80dp 改为 160dp

## Impact
- Affected code: FinanceScreen.kt（FAB modifier + LazyColumn spacer）
- New files: 无

---

## MODIFIED Requirements

### Requirement: FAB 位置偏移
系统 SHALL 将 FAB 从屏幕底部偏移 80dp 至导航栏上方。

#### Scenario: FAB 可见
- **WHEN** FinanceScreen 渲染
- **THEN** FAB 显示在导航栏上方 4dp 处
- **AND** 正常可点击