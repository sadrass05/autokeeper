# Checklist

- [x] proguard-rules.pro 包含 org.openxmlformats.schemas.** keep 规则
- [x] ExcelExporter catch(Throwable) 兜底所有异常类型
- [x] MediaStore IS_PENDING 标记正确设置
- [x] Release 构建 APK 导出功能不闪退
- [x] 支出记录右侧金额使用 headlineLarge/headlineMedium 样式
- [x] 首页和记录页金额样式保持一致
- [x] 理财页多选按钮可见且可切换多选模式
- [x] 多选模式下每行有复选框
- [x] 批量删除弹出确认对话框
- [x] 批量删除正确移除多条记录
- [x] 长按持仓弹出加仓/减仓/删除菜单
- [x] 加仓操作正确更新 buyAmount 和 currentValue
- [x] 减仓操作正确减少 buyAmount 和 currentValue
- [x] 导入同名产品自动合并 buyAmount
- [x] 导入新产品正常创建新记录
- [x] 手动添加同名产品正确合并而非重复