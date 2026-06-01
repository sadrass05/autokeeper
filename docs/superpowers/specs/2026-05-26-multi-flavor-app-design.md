# 多版本应用设计文档

**日期**: 2026-05-26  
**版本**: 1.0

## 📋 概述

将现有应用重构为两个独立版本，使用 Android Gradle 的 Product Flavors 功能实现：

- **标准版 (Standard)**: 纯记账功能，无理财
- **Pro 版 (Pro)**: 完整功能，包含理财

---

## 🎯 需求说明

### 版本对比

| 特性 | 标准版 | Pro 版 |
|------|--------|--------|
| 应用 ID | `com.example.autobookkeeper` | `com.example.autobookkeeper.pro` |
| 应用名称 | 自动记账助手 | 自动记账助手 Pro |
| 记账功能 | ✅ | ✅ |
| 理财功能 | ❌ | ✅ |
| 理财分类 | ❌ | ✅ |
| 理财数据导出 | ❌ | ✅ |
| 图标 | 图标 A | 图标 B |

### 功能差异详情

#### 1. 底部导航栏
- **标准版**: 首页、记录、设置（3 个 tab）
- **Pro 版**: 首页、理财、记录、设置（4 个 tab）

#### 2. 数据库
- **标准版**: 不含 `finance_position` 和 `finance_expense` 表
- **Pro 版**: 完整数据库

#### 3. 记账分类
- **标准版**: 不含理财相关分类
- **Pro 版**: 完整分类

#### 4. 导出功能
- **标准版**: 仅导出记账数据
- **Pro 版**: 支持导出理财数据

---

## 🏗️ 架构设计

### 目录结构

```
app/
├── src/
│   ├── main/                          # 共享代码
│   │   ├── java/com/example/autobookkeeper/
│   │   │   ├── data/
│   │   │   │   ├── dao/              # 共享 DAO
│   │   │   │   ├── entity/           # 共享实体
│   │   │   │   └── repository/       # 共享 Repository
│   │   │   ├── ui/
│   │   │   │   ├── components/       # 共享组件
│   │   │   │   ├── screen/           # 共享屏幕
│   │   │   │   └── theme/            # 共享主题
│   │   │   └── MainActivity.kt       # 主 Activity
│   │   └── res/                      # 共享资源
│   │
│   ├── standard/                      # 标准版特有
│   │   ├── java/com/example/autobookkeeper/
│   │   │   ├── data/
│   │   │   │   ├── dao/              # 标准版 DAO（无理财）
│   │   │   │   ├── entity/           # 标准版实体（无理财）
│   │   │   │   └── AppDatabase.kt    # 标准版数据库
│   │   │   ├── ui/
│   │   │   │   ├── screen/           # 标准版屏幕
│   │   │   │   └── viewmodel/        # 标准版 ViewModel
│   │   │   └── App.kt                # 标准版 Application
│   │   └── res/                      # 标准版资源（图标等）
│   │
│   └── pro/                           # Pro 版特有
│       ├── java/com/example/autobookkeeper/
│       │   ├── data/
│       │   │   ├── dao/              # Pro 版 DAO（含理财）
│       │   │   ├── entity/           # Pro 版实体（含理财）
│       │   │   └── AppDatabase.kt    # Pro 版数据库
│       │   ├── ui/
│       │   │   ├── screen/           # Pro 版屏幕
│       │   │   └── viewmodel/        # Pro 版 ViewModel
│       │   └── App.kt                # Pro 版 Application
│       └── res/                      # Pro 版资源（图标等）
```

### Build.gradle 配置

使用 productFlavors 定义两个版本：

```gradle
flavorDimensions "version"
productFlavors {
    standard {
        dimension "version"
        applicationId "com.example.autobookkeeper"
        versionName "1.0"
        resValue "string", "app_name", "自动记账助手"
    }
    pro {
        dimension "version"
        applicationId "com.example.autobookkeeper.pro"
        versionName "1.0-pro"
        resValue "string", "app_name", "自动记账助手 Pro"
    }
}
```

---

## 📝 详细实现方案

### 1. 重构数据层

#### 共享实体 (main/)
- `Category.kt`
- `ExpenseRecord.kt`

#### Pro 版特有实体 (pro/)
- `FinancePosition.kt`
- `FinanceExpense.kt`

#### 数据库定义
- **standard/AppDatabase.kt**: 仅包含记账相关表
- **pro/AppDatabase.kt**: 包含所有表

### 2. 重构 UI 层

#### 导航栏配置
- 创建 `NavigationConfig.kt` 接口
- standard 版本实现返回 3 个 tab
- pro 版本实现返回 4 个 tab

#### 屏幕组件
- `HomeScreen.kt`: 共享（需要调整，移除理财相关展示）
- `FinanceScreen.kt`: 仅 pro 版本有
- `RecordsScreen.kt`: 共享
- `SettingsScreen.kt`: 共享

### 3. 图标资源

为两个版本准备独立的图标文件夹：

```
app/src/standard/res/mipmap-*/ic_launcher.png
app/src/standard/res/mipmap-*/ic_launcher_round.png
app/src/pro/res/mipmap-*/ic_launcher.png
app/src/pro/res/mipmap-*/ic_launcher_round.png
```

### 4. 功能隔离

使用编译时检查或接口抽象来隔离理财相关代码。

---

## 🔧 实施步骤

1. **配置 Product Flavors**
2. **重构目录结构**
3. **移动和拆分代码文件**
4. **实现导航栏配置**
5. **配置图标资源**
6. **测试两个版本**

---

## ✅ 验收标准

- [ ] 可以分别编译 standard 和 pro 两个版本
- [ ] standard 版本没有理财功能
- [ ] pro 版本有完整理财功能
- [ ] 两个版本使用不同的应用 ID 和图标
- [ ] 两个版本可以同时安装在同一设备上
- [ ] 数据不共享（因为应用 ID 不同）
