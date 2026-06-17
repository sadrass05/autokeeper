# 📦 APK 发布目录

本目录包含由 Gradle 构建命令生成的 release APK 文件。

## 📊 当前版本

| 版本 | 包名 | versionCode | versionName | 大小 |
|:----:|:----:|:-----------:|:-----------:|:----:|
| Standard | `com.example.autobookkeeper` | 2 | 1.0.0 | ~17.7 MB |
| Pro | `com.example.autobookkeeper.pro` | 2 | 1.0.0-pro | ~17.8 MB |

## 🛠️ 构建命令

```bash
# Standard 版
./gradlew assembleStandardRelease

# Pro 版
./gradlew assembleProRelease
```

构建产物位于 `app/build/outputs/apk/{standard,pro}/release/`，本目录的文件是从该目录复制而来的。

## 📥 安装方法

### 通过 ADB 安装

```bash
# Standard 版
adb install -r apk/standard/app-standard-release.apk

# Pro 版（可与 Standard 共存，包名不同）
adb install -r apk/pro/app-pro-release.apk
```

### 手动安装

将 APK 文件传输到 Android 设备，点击安装即可（需要在系统设置中允许"安装未知来源应用"）。

## 🔐 签名信息

APK 使用项目根目录的 `autobookkeeper.jks` 签名（已在 `.gitignore` 中排除，不会进入版本控制）。

- v1 签名：✅
- v2 签名：✅

## 🔗 从 GitHub 直接下载

| 版本 | 下载链接 |
|:----:|:--------:|
| Standard | https://github.com/sadrass05/autokeeper/raw/main/apk/standard/app-standard-release.apk |
| Pro | https://github.com/sadrass05/autokeeper/raw/main/apk/pro/app-pro-release.apk |

## 🆚 版本差异

| 功能 | Standard | Pro |
|:----:|:--------:|:---:|
| 🔔 通知监听自动记账 | ✅ | ✅ |
| 📊 支出趋势图表 | ✅ | ✅ |
| 🥧 分类饼图 | ✅ | ✅ |
| 📤 CSV 导入导出 | ✅ | ✅ |
| 💾 每周自动备份 | ✅ | ✅ |
| 🌙 深色/浅色主题 | ✅ | ✅ |
| ✅ 数据一致性校验 | ✅ | ✅ |
| 🗑️ 软删除回收站 | ✅ | ✅ |
| 🛡️ NLS 健康监测 | ✅ | ✅ |
| 📱 ROM 引导设置 | ✅ | ✅ |
| 💹 理财持仓管理 | ❌ | ✅ |
| 📈 收益排行榜 | ❌ | ✅ |
| 🗄️ MySQL 数据同步 | ❌ | ✅ |
| 📤 理财数据导出 | ❌ | ✅ |

## ⚠️ 安全提示

1. **首次安装前** 需允许"安装未知来源应用"
2. **Pro 版与 Standard 版** 可以共存（包名不同）
3. **覆盖安装** Standard 版不会影响 Pro 版数据
4. **APK 文件较大** 是因为包含 Compose、Hilt、ML Kit 等完整运行时

## 🆚 与 GitHub Releases 的关系

**当前已配置 GitHub Releases**（v1.0.0、v1.0.0-pro），推荐使用 Release 链接下载：

- **Standard:** https://github.com/sadrass05/autokeeper/releases/download/v1.0.0/app-standard-release.apk
- **Pro:** https://github.com/sadrass05/autokeeper/releases/download/v1.0.0-pro/app-pro-release.apk
- **Latest:** https://github.com/sadrass05/autokeeper/releases/latest

仓库内 `apk/` 目录作为备份分发方式保留，新版本请优先通过 Release 分发，以获得：
- 📌 版本号管理（v1.0.0, v1.0.1, ...）
- 📋 Release Notes
- 📊 下载统计
- 🔐 SHA256 校验和
- 🏷️ 预发布版本（alpha/beta/rc）

## 📂 目录结构

```
apk/
├── README.md                    # 本文件
├── standard/
│   ├── app-standard-release.apk # Standard 版 APK
│   └── output-metadata.json     # Gradle 构建元数据
└── pro/
    ├── app-pro-release.apk      # Pro 版 APK
    └── output-metadata.json     # Gradle 构建元数据
```

## 🔄 更新流程

修改代码后，重新构建并更新此目录：

```bash
# 1. 重新构建
./gradlew clean assembleStandardRelease assembleProRelease

# 2. 复制新 APK
cp app/build/outputs/apk/standard/release/app-standard-release.apk apk/standard/
cp app/build/outputs/apk/pro/release/app-pro-release.apk apk/pro/

# 3. 提交并推送
git add apk/
git commit -m "build: update release APK files"
git push origin master
```
