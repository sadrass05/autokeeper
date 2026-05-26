# Checklist

- [x] topExpenses StateFlow 使用 stateIn 从 expenses 派生，过滤本月数据
- [x] topExpenses 按金额降序排列且取前5条
- [x] SharingStarted 和 stateIn 已正确导入 MainViewModel
- [x] TopExpensesCard.kt 创建于 ui/components/ 目录下
- [x] RankBadge 实现3级样式：第1名主题色圆+白字、第2-3名浅灰圆+深字、第4-5名纯文字
- [x] CategoryIconCircle 实现40dp圆形 + 分类首字符 + 颜色映射
- [x] formatRecordDate 将时间戳格式化为 "MM-dd HH:mm"
- [x] getCategoryColor 根据分类名返回对应颜色（10色映射+5色fallback）
- [x] 每条记录 Row 布局：序号→图标圆→商户名+分类→金额+日期
- [x] 商户名 14sp Medium，分类 11sp onSurfaceVariant
- [x] 金额 15sp SemiBold error色，日期 11sp onSurfaceVariant
- [x] 卡片底部有"查看全部记录 →"按钮
- [x] 卡片标题为"本月支出排行"
- [x] HomeScreen 新增 onNavigateToRecords 回调参数
- [x] 排行榜卡片位于月度对比图表之后、当日支出分类之前
- [x] topExpenses 空时不渲染卡片
- [x] MainActivity 传递 onNavigateToRecords = { selectedScreen = 1 }
- [x] 无编译错误，无未使用导入