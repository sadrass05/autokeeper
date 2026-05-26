# Checklist

- [x] ExpenseRecord 实体包含 isDeleted 字段
- [x] Room 数据库 V1→V2 迁移正确添加 is_deleted 列
- [x] 所有 DAO 查询过滤 isDeleted = false
- [x] 统计计算排除已删除记录
- [x] 30 天自动清理逻辑正确
- [x] 滑动到底弹出删除确认对话框
- [x] 确认后记录 isDeleted 变为 true
- [x] 取消后记录恢复原位
- [x] 设置页有"回收站"入口
- [x] 回收站正确显示已删除记录
- [x] 恢复按钮将 isDeleted 设回 false
- [x] 清空回收站真删数据
- [x] 记录列表副标题显示日期时间
- [x] 首页最近交易副标题显示日期时间
- [x] 理财多选有全选按钮
- [x] 全选/取消全选切换正常
