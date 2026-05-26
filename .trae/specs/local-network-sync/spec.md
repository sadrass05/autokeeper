# 本地网络数据同步 Spec

## Why
当前"数据库同步"功能使用 JDBC 直连 MySQL，Android 系统限制无法实现。需要改为 HTTP REST API 方案：手机通过 Retrofit 发送数据到电脑上的轻量 HTTP 服务，由电脑服务写入 MySQL。

## What Changes
- **修改** `network/ApiService.kt` — 添加 `/ping`、增量同步接口
- **修改** `network/MySqlApi.kt` — 改为支持动态 baseUrl 的完整同步客户端
- **修改** `di/NetworkModule.kt` — 支持运行时切换 baseUrl
- **新增** `network/SyncService.kt` — 同步管理器（全量/增量、进度、错误处理）
- **新增** `data/SyncPrefs.kt` — SharedPreferences 存储服务端 IP、端口、上次同步时间
- **修改** `ui/screen/SettingsScreen.kt` — 添加数据同步 UI（IP 输入、测试连接、全量/增量同步按钮、进度、上次同步时间）
- **新增** `server/sync_server.py` — Python Flask 电脑端服务脚本
- **新增** `server/requirements.txt` — Python 依赖

## Impact
- Affected specs: 无（新功能）
- Affected code:
  - `network/ApiService.kt`
  - `network/MySqlApi.kt`
  - `di/NetworkModule.kt`
  - `ui/screen/SettingsScreen.kt`
  - `ui/viewmodel/MainViewModel.kt`（可能需要调整 syncToMySQL 方法）

## ADDED Requirements

### Requirement: 服务器连接配置
系统 SHALL 允许用户在 SettingsScreen 中输入电脑的局域网 IP 地址和端口并持久化保存。

#### Scenario: 保存服务器配置
- **GIVEN** 用户输入 IP "192.168.1.100" 和端口 "5000"
- **WHEN** 用户离开输入框或切换到其他页面
- **THEN** IP 和端口被保存到 SharedPreferences，下次打开设置页自动回显

### Requirement: 连接测试
系统 SHALL 提供"测试连接"按钮，GET /ping 验证服务器可达性。

#### Scenario: 连接成功
- **GIVEN** 电脑服务已启动且手机在同一 WiFi
- **WHEN** 用户点击"测试连接"
- **THEN** 显示绿色提示"连接成功"

#### Scenario: 连接失败
- **GIVEN** 电脑服务未启动或不在同一网络
- **WHEN** 用户点击"测试连接"
- **THEN** 显示红色提示"请确保电脑服务已启动且与手机在同一WiFi下"

### Requirement: 全量同步
系统 SHALL 支持全量同步：将所有本地支出记录和理财持仓发送到服务器。

#### Scenario: 全量同步成功
- **GIVEN** 连接正常，本地有 50 条支出记录和 10 条理财持仓
- **WHEN** 用户点击"全量同步"
- **THEN** ProgressIndicator 展示进度，完成后显示"成功同步 50 条支出记录，10 条理财记录"

### Requirement: 增量同步
系统 SHALL 支持增量同步：仅发送上次同步时间之后新增或修改的记录。

#### Scenario: 增量同步只发送新数据
- **GIVEN** 上次同步时间为 T，此后新增 5 条支出记录
- **WHEN** 用户点击"增量同步"
- **THEN** 仅发送这 5 条记录，完成后显示"成功同步 5 条支出记录，0 条理财记录"

### Requirement: 同步进度显示
系统 SHALL 在同步过程中显示 ProgressIndicator 或 LinearProgressIndicator。

### Requirement: 上次同步时间显示
系统 SHALL 在设置页显示上次成功同步的时间戳（格式 "yyyy-MM-dd HH:mm:ss"）。

### Requirement: 超时处理
系统 SHALL 设置 OkHttp 连接超时为 5 秒、读写超时为 30 秒，超时时给用户友好提示。

### Requirement: 电脑端服务
系统 SHALL 提供 Python Flask 服务脚本，仅需 `pip install -r requirements.txt && python sync_server.py` 即可启动。

#### Scenario: 服务启动
- **GIVEN** MySQL 已运行，数据库名 autobookkeeper
- **WHEN** 执行脚本
- **THEN** 服务监听 0.0.0.0:5000，自动建表，打印启动信息

#### Scenario: 接收数据
- **WHEN** 服务收到 POST /sync/expenses
- **THEN** 使用 REPLACE INTO 批量写入 MySQL

### Requirement: 错误友好处理
系统 SHALL 对所有网络错误提供中文友好提示，不 crash。

## REMOVED Requirements

### Requirement: JDBC 直连 MySQL 同步
**Reason**: Android 不支持 JDBC 直连 MySQL
**Migration**: 替换为 HTTP REST API 方案