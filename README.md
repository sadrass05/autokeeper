# 🪙 自动记账助手（AutoBookkeeper）

> 让每一笔消费都有迹可循 —— 一个独立开发者的记账 Side Project

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green)](https://github.com/sadrass05/autokeeper)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## ✨ 为什么做这个

说实话，我是个**很懒的人**。

每次消费完都要打开记账 App、选分类、输金额、确认保存 —— 这套流程我坚持了大概... 两天。然后就没然后了。

直到有一天我发现 Android 有个 **NotificationListenerService**，可以合法读取手机上的通知内容。微信支付、支付宝、云闪付 —— 它们付款后都会弹一条通知，里面清清楚楚写着付给了谁、花了多少钱。

> **那为什么不直接把通知抓下来自动记？**

于是就有了这个 App。从最开始的几十行代码跑通原型，到后来加上图表、理财持仓管理、数据同步…… 一路边用边改，慢慢长成了现在的样子。

它不是一个完美的商业产品，但它是**我自己每天在用的工具**。如果你也厌倦了手动记账，也许它能帮到你。

---

## 📱 功能特性

### 核心功能

- 🔔 **通知监听自动记账** —— 微信、支付宝、云闪付等主流支付平台，付款后自动记录，零操作
- 📊 **支出趋势可视化** —— 7 天折线图 + 月度柱状图，一眼看穿钱去哪了
- 🥧 **当日支出分类饼图** —— 餐饮、交通、购物…… 各占多少一目了然
- 📅 **按天分组的交易记录** —— 时间线式浏览，像翻聊天记录一样自然
- 🗑️ **回收站（软删除）** —— 误删不怕，随时恢复

### Pro 版专属 💎

- 💹 **理财持仓管理** —— 基金、股票、定期，统一跟踪
- 📈 **收益排行榜** —— 哪只赚得多、哪只在亏钱，排名说话
- 🗄️ **MySQL 数据同步** —— 局域网内同步到自建数据库，多端查看
- 📤 **理财数据 CSV 导出** —— 持仓和收益记录一键导出

### 通用功能

- 💾 **每周自动本地备份** —— WorkManager 定时任务，CSV 导出到私有目录
- ✅ **数据一致性校验** —— 启动时自动对比 app 数据与最新备份的记录数
- 📤 **CSV 导入导出** —— Excel 可直接打开，换机 / 重装不丢数据
- 🌙 **深色 / 浅色主题** —— 跟随系统或手动切换，护眼模式安排上

---

## 🛠️ 技术栈

| 层级 | 技术选型 | 为什么用它 |
|------|----------|-----------|
| UI | **Jetpack Compose + Material3** | 声明式 UI，写起来爽，动画天然流畅 |
| 架构 | **MVVM + Hilt DI** | ViewModel 管理状态，Hilt 自动注入，少写样板代码 |
| 本地数据库 | **Room + Flow** | Flow 响应式查询，UI 自动更新，不用手动 refresh |
| 网络 | **Retrofit2 + OkHttp3** | Pro 版同步 MySQL 用，声明式 API 定义 |
| 图表 | **MPAndroidChart** | 折线图、柱状图、饼图、环形图全靠它 |
| 后台任务 | **WorkManager** | 每周定时备份，系统杀不死 |
| 构建变体 | **Product Flavors (standard/pro)** | 同一套代码，两个版本，`BuildConfig.IS_PRO` 控制 |
| 混淆 | **R8 (ProGuard)** | Release 包全量混淆 + 资源压缩，APK 缩 ~30% |

---

## 🚀 快速开始

### 环境要求

- **Android 8.0+** (API 26)
- 需要开启 **通知监听权限**（设置 → 应用 → 通知访问）
- Pro 版数据同步需要 **局域网环境**

### 安装

直接安装 APK：

```bash
# Standard 版本（基础记账）
app/build/outputs/apk/standard/release/app-standard-release.apk

# Pro 版本（完整功能）
app/build/outputs/apk/pro/release/app-pro-release.apk
```

或通过 Android Studio 打开项目，选择 `standardDebug` / `proDebug` 变体运行。

### 数据同步（Pro 版）

Pro 版支持将数据同步到自建 MySQL 数据库。需要在同一局域网的电脑上运行服务端：

```bash
pip install flask pymysql
python server/sync_server.py
```

然后在 App 设置页填入电脑的局域网 IP 和端口即可。

> ⚠️ 仅限局域网通信，数据不会上传到公网服务器。网络安全配置已默认禁止明文 HTTP 流量，仅放行私有 IP 段。

---

## 📂 项目结构

```
app/src/
├── main/                          # 主源码（两个版本共用）
│   ├── java/com/example/autobookkeeper/
│   │   ├── backup/                # 备份系统
│   │   │   ├── BackupManager.kt       # CSV 备份/恢复/校验
│   │   │   └── WeeklyBackupWorker.kt  # WorkManager 定时任务
│   │   ├── data/                  # 数据层
│   │   │   ├── dao/              # Room DAO 接口
│   │   │   ├── entity/           # 数据实体（含软删除字段）
│   │   │   └── repository/       # Repository 封装
│   │   ├── di/                   # Hilt 依赖注入模块
│   │   ├── notification/         # 通知监听 & 支付解析
│   │   │   ├── NotificationListener.kt
│   │   │   └── PaymentParser.kt      # 黑名单过滤 + 正则提取
│   │   ├── ui/
│   │   │   ├── components/       # Compose 通用组件（玻璃态卡片、图表等）
│   │   │   ├── screen/           # 页面（首页/明细/设置/回收站）
│   │   │   ├── theme/            # Material3 主题定制
│   │   │   └── viewmodel/        # MVVM ViewModel
│   │   ├── App.kt               # Application 入口 + WorkManager 初始化
│   │   ├── MainActivity.kt      # 单 Activity + Compose Navigation
│   │   └── FlavorConfig.kt      # 版本差异配置接口
│   └── res/xml/
│       └── network_security_config.xml   # 网络安全策略
│
├── pro/                           # Pro 版专属代码
│   └── java/com/example/autobookkeeper/
│       ├── data/                  # 理财相关 Entity/DAO/Repository
│       ├── network/              # MySQL 同步（Retrofit + ApiService）
│       ├── ui/
│       │   ├── export/CsvExporter.kt     # 含理财持仓导出
│       │   ├── importdata/               # 含理财数据导入
│       │   └── screen/FinanceScreen.kt   # 理财页面
│       └── di/DatabaseModule.kt          # Pro 版 Room Database
│
└── standard/                      # Standard 版 stub 实现
    └── java/com/example/autobookkeeper/
        ├── data/, network/, ui/  # 空实现 / no-op stub
        └── screen/FinanceScreen.kt  # 显示"标准版暂不支持"

server/
    └── sync_server.py             # Flask 数据同步服务端
```

---

## 🗺️ 开发路线图

- [ ] **预算管理** —— 月度预算上限 + 超支提醒
- [ ] **更多支付平台** —— 美团、抖音、京东白条等
- [ ] **数据可视化增强** —— 年度报表、分类趋势热力图
- [ ] **Widget 桌面小组件** —— 今日支出一览
- [ ] **iOS 版本** —— 远期梦想（SwiftUI + Flutter?）

---

## 📝 隐私说明

这个 App 的设计原则是 **数据主权归用户所有**：

- 📱 通知读取权限 **仅用于识别支付消息**，不会读取聊天内容或其他隐私信息
- 💾 所有数据存储在 **设备本地**（Room 数据库），不会上传到任何远程服务器
- 🌐 Pro 版的网络通信 **仅在局域网内进行**，网络安全配置默认阻止所有明文 HTTP
- 🔒 Release 包经过 R8 全量混淆，逆向难度较高
- 🚫 不含任何第三方统计 SDK（无 Google Analytics、无友盟、无埋点）

简单说：**你的账单只有你能看到**。

---

## 🤝 贡献

欢迎提 Issue 和 PR！特别需要帮助的方向：

- 🔍 **新增支付平台的解析规则** —— `PaymentParser.kt` 中的正则匹配
- 🎨 **UI/UX 改进建议** —— Compose 组件优化、交互体验提升
- 🐛 **Bug 报告** —— 特别是不同机型 / Android 版本的兼容性问题
- 📖 **文档补全** —— 目前文档确实比较少（独立开发者的通病 😅）

---

## 📄 License

MIT License © 2026 [sadrass05](https://github.com/sadrass05)

> 用着开心的话，给个 ⭐ 就是对我最大的鼓励 🙏
