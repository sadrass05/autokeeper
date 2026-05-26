# 网络同步连通性修复 + 自诊断功能 实施计划

## 根因分析

通过对代码库的全面审查，识别出以下 3 个导致 App 连接 Flask 服务器失败的问题：

| # | 问题 | 位置 | 影响 | 概率 |
|---|------|------|------|------|
| 1 | **缺少 `usesCleartextTraffic`** | `AndroidManifest.xml` 第 11 行 `<application>` 标签 | 所有 HTTP 请求被系统静默拦截，`ConnectException` | ~90% |
| 2 | **API 路由不匹配** | `ApiService.kt` vs `sync_server.py` | 同步请求 404（ping 不受影响，路由正确） | ~100% |
| 3 | **Flask 无请求日志** | `sync_server.py` 第 123 行 `debug=False` | 请求到达也无法在控制台看到 | ~100% |

### 问题 1 详解

Android 9 (API 28) 起默认禁止 cleartext HTTP 流量。项目 `targetSdk = 35`，所有 `http://` 请求被 NetworkSecurityPolicy 拦截。浏览器不受影响因为 WebView 有独立的网络安全配置。

**解决方案**：在 `<application>` 标签添加 `android:usesCleartextTraffic="true"`。

### 问题 2 详解

```
Retrofit ApiService 路径     Flask Route
─────────────────────────     ────────────
@GET("ping")               →  @app.route('/ping')           ✅ 匹配
@POST("api/expenses/sync") →  @app.route('/sync/expenses')  ❌ 不匹配 → 404
@POST("api/finance/sync")  →  @app.route('/sync/positions') ❌ 不匹配 → 404
```

**解决方案**：统一路由为 `/sync/expenses` 和 `/sync/positions`（Flask 端保持不变，改 App 端）。同时删除未使用的 `@GET("api/expenses/list")` 和 `@GET("api/finance/list")` 接口。

### 问题 3 详解

`app.run(debug=False)` 不输出请求日志。即使问题 1 和 2 修复后，控制台也看不到请求详情。

**解决方案**：添加 Flask 请求日志中间件 + 异常时输出完整 traceback。

---

## 变更范围

### 修改的文件（5 个）

| 文件 | 类型 | 变更 |
|------|------|------|
| `app/src/main/AndroidManifest.xml` | Android | 添加 `usesCleartextTraffic="true"` |
| `app/src/main/java/.../network/ApiService.kt` | Android | 修复路由路径 |
| `app/src/main/java/.../network/SyncService.kt` | Android | 新增 `runDiagnostics()` 自诊断方法 |
| `app/src/main/java/.../ui/screen/SettingsScreen.kt` | Android | 集成诊断 UI |
| `server/sync_server.py` | Python | 添加请求日志 + CORS + 错误详情 |

---

## 具体实施步骤

### Step 1：修复 AndroidManifest.xml — cleartextTraffic

**文件**：`app/src/main/AndroidManifest.xml`

在 `<application` 标签中添加一行：

```xml
<application
    android:name=".App"
    android:allowBackup="true"
    android:usesCleartextTraffic="true"
    ...>
```

此属性允许 App 在所有网络类型下发送 HTTP 明文请求，是 targetSdk 28+ 使用 HTTP 的必需配置。

---

### Step 2：修复 ApiService.kt — 路由对齐

**文件**：`app/src/main/java/com/example/autobookkeeper/network/ApiService.kt`

将 sync 路由从 `api/expenses/sync` / `api/finance/sync` 改为 `/sync/expenses` / `/sync/positions` 匹配 Flask。

删除未使用的 `@GET("api/expenses/list")` 和 `@GET("api/finance/list")`。

修改后：
```kotlin
interface ApiService {
    @GET("ping")
    suspend fun ping(): PingResponse

    @POST("sync/expenses")
    suspend fun syncExpenses(@Body request: SyncExpensesRequest): SyncResponse

    @POST("sync/positions")
    suspend fun syncFinance(@Body request: SyncFinanceRequest): SyncResponse
}
```

---

### Step 3：SyncService.kt — 新增自诊断方法

**文件**：`app/src/main/java/com/example/autobookkeeper/network/SyncService.kt`

新增 `data class DiagnosticsResult` 和 `suspend fun runDiagnostics(ip, port): List<DiagnosticsStep>`。

诊断流程分 4 步：
1. **地址格式验证** — 检查 IP 和端口是否合法
2. **DNS 解析测试** — `InetAddress.getByName(ip)` 是否成功
3. **TCP 端口连通性** — `Socket(ip, port)` 3 秒超时测试
4. **HTTP Ping 请求** — 通过 Retrofit 发送 GET /ping

每步返回 `DiagnosticsStep(name, success, detail)`。

---

### Step 4：SettingsScreen.kt — 集成诊断 UI

**文件**：`app/src/main/java/com/example/autobookkeeper/ui/screen/SettingsScreen.kt`

在现有"测试连接"按钮下方添加：
- 诊断按钮："详细诊断"
- 诊断结果面板：4 步骤每步用 ✅/❌ 图标 + 详情文字展示
- 只在用户主动点击时执行诊断（避免自动触发）
- 诊断结果可折叠/展开

保持现有 `ping` 函数不变，`runDiagnostics` 作为额外的深度诊断途径。

---

### Step 5：sync_server.py — 请求日志 + CORS

**文件**：`server/sync_server.py`

变更点：
1. 添加 `@app.before_request` 中间件：记录每个请求的方法、路径、来源 IP
2. 添加 `@app.after_request`：记录响应状态码
3. 添加 CORS 头（`Access-Control-Allow-Origin: *`），防止 WebView 场景下的跨域问题
4. 异常处理改为输出 `traceback.format_exc()` 到控制台
5. `debug=False` 保持不变（生产安全），但日志通过 `print()` 输出

日志格式示例：
```
[2026-05-24 10:30:45] GET /ping from 192.168.43.1 → 200
[2026-05-24 10:30:50] POST /sync/expenses from 192.168.43.1 → 200 (12 records)
```

---

## 安全考量

- `usesCleartextTraffic="true"` 仅放开 HTTP，不涉及其他安全降级
- 局域网同步场景下 HTTP 可接受（手机热点隔离环境）
- 如需公网部署，后续可升级为 HTTPS + 自签名证书 + Network Security Config
- 不做 HSTS 配置（不适合动态 IP 的局域网场景）

---

## 验证方法

修复后验证流程：
1. 启动电脑端 Flask 服务，观察控制台是否输出 `服务已启动`
2. App 输入 IP `192.168.43.100` + 端口 `5000`
3. 点击「测试连接」— 期望看到 `连接成功`
4. 点击「详细诊断」— 期望 4 步全部 ✅
5. 如果某步失败，查看对应 detail 信息定位问题
6. 同步数据 — 期望 Flask 控制台输出请求日志

---

## 决策记录

| 决策 | 理由 |
|------|------|
| 只改 Android 端路由，不改 Flask 端 | Flask 路由更 RESTful（`/sync/expenses`），保持后端稳定 |
| 自诊断集成到 SettingsScreen | 用户选择，避免增加新的页面/导航复杂度 |
| 诊断 4 步渐进式 | 从网络层→传输层→应用层逐层排查，精准定位问题层级 |
| Flask 日志用 `print()` 而非 `logging` | 保持依赖简洁，本地开发环境 `print()` 足够 |
