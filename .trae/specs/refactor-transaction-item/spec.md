# TransactionItem 卡片组件重构 Spec

## Why
当前 TransactionItem 使用简单的 `Row + background + clip` 布局，缺少 Card 包裹、阴影、图标区域、金额色彩语义（支出红/收益绿），视觉效果简陋。

## What Changes
- TransactionItem 改为 Card + 自定义 shadow 包裹
- 新增左侧 40dp 圆形图标区域（surfaceVariant 背景 + 分类首字）
- 文字层级升级：商户名 titleMedium、平台 bodySmall、金额 titleMedium Bold + 色彩语义、时间 labelSmall
- 金额按正负值使用 error（支出）或 tertiary（收益）色
- 新增 onClick 支持（默认 ripple 水波纹）
- LazyColumn spacing 改为 10dp，contentPadding 改为 horizontal 16dp/vertical 12dp
- 移除 TransactionItem 之间的 Spacer + HorizontalDivider 分隔

## Impact
- Affected specs: ui-overhaul, material3-theme-system
- Affected code: HomeScreen.kt（TransactionItem 函数 + LazyColumn 参数）
- New files: 无

---

## ADDED Requirements

### Requirement: Card 包裹与自定义阴影
系统 SHALL 使用 Card(shape=16dp, elevation=0dp) 包裹 TransactionItem，配合 Modifier.shadow(4dp, spotColor=Black@0.06) 实现柔和投影。

#### Scenario: 卡片渲染
- **WHEN** 渲染一根 TransactionItem
- **THEN** shape = RoundedCornerShape(16.dp)
- **AND** elevation = 0.dp（不使用 Material 默认阴影）
- **AND** 外部 shadow(4dp, RoundedCornerShape(16.dp), ambientColor=Black@0.06, spotColor=Black@0.06)
- **AND** 背景色使用 MaterialTheme.colorScheme.surface

### Requirement: 左侧图标区域
系统 SHALL 在卡片左侧显示 40dp 圆形类别图标。

#### Scenario: 图标显示
- **WHEN** 渲染交易项
- **THEN** 左对齐 40dp 圆形 Box，background = surfaceVariant
- **AND** 内容居中显示分类名称首字符
- **AND** 首字符使用 titleMedium + onSurfaceVariant

### Requirement: 文字层级
系统 SHALL 使用明确的 4 级文字层级渲染交易信息。

#### Scenario: 商户名
- **WHEN** 渲染商户名
- **THEN** 使用 titleMedium + onSurface

#### Scenario: 平台信息
- **WHEN** 渲染平台和支付渠道
- **THEN** 使用 bodySmall + onSurfaceVariant

#### Scenario: 金额
- **WHEN** 渲染金额
- **THEN** 使用 titleMedium + FontWeight.Bold
- **AND** 支出（amount>0）：error 色 + `-¥` 前缀
- **AND** 收益（amount<0）：tertiary 色 + `+¥` 前缀（取绝对值）

#### Scenario: 时间
- **WHEN** 渲染交易时间
- **THEN** 使用 labelSmall + onSurfaceVariant

### Requirement: Card onClick
系统 SHALL 为 TransactionItem 添加可选的点击事件。

#### Scenario: 点击效果
- **WHEN** 用户点击交易卡片
- **THEN** 触发默认 Material ripple 水波纹效果
- **AND** 执行 onClick 回调

### Requirement: LazyColumn 间距标准化
最近交易列表 SHALL 使用统一的间距规范。

#### Scenario: 列表间距
- **WHEN** 渲染交易列表 LazyColumn
- **THEN** verticalArrangement = Arrangement.spacedBy(10.dp)
- **AND** contentPadding = PaddingValues(horizontal=16.dp, vertical=12.dp)

## MODIFIED Requirements

### Requirement: TransactionItem 布局（替代原版）
系统 SHALL 使用 Card + shadow + Row(图标+文字+金额) 布局替代原 Row+clip+background 布局。

#### Scenario: 整体布局
- **WHEN** 渲染交易项
- **THEN** 卡片内边距 horizontal=16dp, vertical=14dp
- **AND** 图标与文字间距 = 12dp
- **AND** 文字列使用 Modifier.weight(1f) 占据中间空间
- **AND** 金额右对齐