# Tasks

- [x] Task 1: 创建电脑端 Flask 服务脚本
  - [x] 创建 `server/sync_server.py`：Flask 服务，监听 0.0.0.0:5000
  - [x] GET /ping → `{"status": "ok"}`
  - [x] POST /sync/expenses → 接收 ExpenseRecord JSON 数组，REPLACE INTO 批量写入 MySQL expenses 表
  - [x] POST /sync/positions → 接收 FinancePosition JSON 数组，REPLACE INTO 批量写入 MySQL finance_positions 表
  - [x] 自动建表（启动时 CREATE TABLE IF NOT EXISTS）
  - [x] 创建 `server/requirements.txt`（Flask, PyMySQL 等依赖）

- [x] Task 2: 创建 SyncPrefs（SharedPreferences 配置存储）
  - [x] 创建 `data/SyncPrefs.kt`：使用 SharedPreferences 存储 serverIp、serverPort、lastSyncTime
  - [x] 提供 save/get 方法，默认值 IP=""、port=5000、lastSyncTime=0L

- [x] Task 3: 增强 ApiService 接口
  - [x] 添加 `@GET("ping") suspend fun ping(): PingResponse`
  - [x] 添加 `data class PingResponse(val status: String)`
  - [x] 添加 `data class SyncResponse(val success: Boolean, val message: String)`
  - [x] 修改 `POST sync/expenses` 和 `POST sync/positions` 返回 `SyncResponse`
  - [x] 保留现有接口兼容性

- [x] Task 4: 创建 SyncService（同步管理器）
  - [x] 创建 `network/SyncService.kt`（普通类，Hilt 注入）
  - [x] 构造函数参数：Context（用于 SharedPreferences）
  - [x] `suspend fun ping(ip: String, port: Int): String` — 创建临时 Retrofit 实例测试连接
  - [x] `suspend fun fullSync(ip, port, expenses, positions, onProgress): SyncResult` — 全量同步
  - [x] `suspend fun incrementalSync(ip, port, expenses, positions, onProgress): SyncResult` — 增量同步
  - [x] `data class SyncResult(val expenseCount, positionCount, success, message)`
  - [x] OkHttp 超时：connect 5s, read 30s, write 30s
  - [x] 异常捕获：ConnectException → "无法连接到服务器"、SocketTimeoutException → "连接超时"

- [x] Task 5: 重构 NetworkModule 支持动态 baseUrl
  - [x] 修改 `provideOkHttpClient()` 设置 connectTimeout=5s, readTimeout=30s, writeTimeout=30s
  - [x] 保留现有 Retrofit 兼容性，SyncService 内自行创建临时 Retrofit

- [x] Task 6: 重构 SettingsScreen — 数据同步 UI
  - [x] 将现有"数据库同步" SettingsRow 替换为完整的同步设置区域
  - [x] 服务器地址输入区域：OutlinedTextField IP + OutlinedTextField 端口
  - [x] IP/端口变更自动保存到 SyncPrefs
  - [x] "测试连接"按钮：OutlinedButton，点击后调用 SyncService.ping()
  - [x] 连接状态文字（绿色"连接成功"/红色失败原因）
  - [x] "全量同步"按钮：FilledTonalButton + LinearProgressIndicator
  - [x] "增量同步"按钮：OutlinedButton + LinearProgressIndicator
  - [x] 同步结果显示文字
  - [x] 上次同步时间显示（从 SyncPrefs 读取并格式化）
  - [x] 同步过程中禁用按钮防止重复点击

- [x] Task 7: 更新 MainViewModel syncToMySQL 方法
  - [x] 标注 `syncToMySQL()` 为废弃方法
  - [x] 更新 MySqlApi 接收 SyncResponse 返回值并检查 success

# Task Dependencies
- Task 4 依赖 Task 2 和 Task 3
- Task 6 依赖 Task 2、Task 4、Task 5
- Task 1 可与其他任务并行
- Task 5 可与其他任务并行