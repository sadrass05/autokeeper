# Tasks

- [x] Task 1: 重构 TransactionItem Composable
  - [x] SubTask 1.1: 用 Card(shape=16dp, elevation=0dp, onClick) 包裹整体，添加 Modifier.shadow(4dp, ambientColor=Black@0.06, spotColor=Black@0.06)
  - [x] SubTask 1.2: 新增左侧 40dp 圆形 Box（surfaceVariant 背景 + 分类首字符居中）
  - [x] SubTask 1.3: 重构文字列：商户名 titleMedium/onSurface，平台 bodySmall/onSurfaceVariant
  - [x] SubTask 1.4: 金额使用 titleMedium Bold，支出 error/-¥，收益 tertiary/+¥
  - [x] SubTask 1.5: 新增时间 labelSmall/onSurfaceVariant 在平台信息行右侧
  - [x] SubTask 1.6: 卡片内边距 horizontal=16dp, vertical=14dp，图标间距 12dp

- [x] Task 2: 调整 HomeScreen 交易列表参数
  - [x] SubTask 2.1: 最近交易区域 Column(verticalArrangement = spacedBy(10.dp)) 等效实现
  - [x] SubTask 2.2: 移除 TransactionItem 之间的 Spacer + HorizontalDivider 分隔

# Task Dependencies
- Task 2 depends on Task 1（需要新 TransactionItem 接口就绪）