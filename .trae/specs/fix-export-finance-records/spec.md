# 导出修复 + 财务增强 Spec

## Why
导出 Excel 在真机上因 R8 剥离 schema 类导致 NoClassDefFoundError 崩溃；支出记录标题缺少金额展示；理财模块缺少批量操作和同种理财自动合并能力。

## What Changes
- 修复 ProGuard/R8 规则保留 `org.openxmlformats.schemas.*`，兜底 catch Throwable
- 支出记录右侧金额放大加粗（headlineMedium）
- 理财页增加多选批量删除、长按菜单（加仓/减仓/删除）、导入/添加时同产品自动合并

## Impact
- Affected specs: fix-six-issues
- Affected code: proguard-rules.pro, ExcelExporter.kt, RecordsScreen.kt, HomeScreen.kt, FinanceScreen.kt, FinanceDao.kt, FinanceRepository.kt, MainViewModel.kt

---

## ADDED Requirements

### Requirement: 导出防崩溃
Excel 导出 SHALL 在所有 Android 版本上稳定运行，不因 R8 代码剥离或权限异常导致崩溃。

#### Scenario: Release 构建正常导出
- **WHEN** 用户点击导出报表（首页或设置页）
- **THEN** Excel 文件成功生成到下载目录
- **AND** 不会因 NoClassDefFoundError 导致 App 闪退

#### Scenario: 异常安全兜底
- **WHEN** 导出过程中发生任何 Throwable 异常
- **THEN** 系统捕获并以 Toast 提示用户错误信息

### Requirement: 支出记录金额突出显示
支出记录列表 SHALL 以更大更粗字体展示金额，便于快速识别。

#### Scenario: 记录列表金额展示
- **WHEN** 用户查看支出记录列表
- **THEN** 右侧金额使用 headlineMedium 样式加粗显示
- **AND** 左侧标题保持分类名

### Requirement: 理财批量删除
理财持仓列表 SHALL 支持多选模式批量删除持仓记录。

#### Scenario: 进入多选模式
- **WHEN** 用户点击标题栏"多选"按钮
- **THEN** 列表每行左侧出现复选框
- **AND** 顶部显示"已选 N 项"和"删除"操作栏

#### Scenario: 批量删除执行
- **WHEN** 用户勾选多项后点击"删除"
- **THEN** 弹出确认对话框
- **AND** 确认后批量删除所选持仓

### Requirement: 持仓长按操作菜单
持仓条目 SHALL 支持长按弹出操作菜单，包含加仓、减仓、删除选项。

#### Scenario: 加仓操作
- **WHEN** 用户长按某持仓，选择"加仓"
- **THEN** 弹出输入框输入加仓金额
- **AND** 确认后 buyAmount 和 currentValue 同步增加

#### Scenario: 减仓操作
- **WHEN** 用户长按某持仓，选择"减仓"
- **THEN** 弹出输入框输入减仓金额
- **AND** 确认后 buyAmount 和 currentValue 同步减少

### Requirement: 同产品自动合并
导入或手动添加持仓时，SHALL 按 productName 自动匹配已有持仓并合并更新，避免创建重复记录。

#### Scenario: 同名产品自动合并
- **WHEN** 导入或添加的持仓 productName 与已有记录相同
- **THEN** 系统自动合并：buyAmount 累加，currentValue/profit/profitRate 更新为新值
- **AND** 不创建重复记录

#### Scenario: 新产品正常新增
- **WHEN** 导入或添加的持仓 productName 不与任何已有记录匹配
- **THEN** 正常插入新记录

## MODIFIED Requirements

### Requirement: 导出报表按钮（修改）
**原行为**：ExcelExporter catch(Exception)，ProGuard 只保留 org.apache.poi.**
**修改后**：ExcelExporter catch(Throwable)；ProGuard 增加 org.openxmlformats.schemas.**；MediaStore 增加 IS_PENDING 标记
