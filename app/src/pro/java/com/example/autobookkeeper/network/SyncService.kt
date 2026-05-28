package com.example.autobookkeeper.network

import android.content.Context
import com.example.autobookkeeper.data.SyncPrefs
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val expenseCount: Int,
    val positionCount: Int,
    val success: Boolean,
    val message: String
)

data class DiagnosticsStep(
    val stepName: String,
    val success: Boolean,
    val detail: String
)

@Singleton
class SyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPrefs: SyncPrefs
) {

    private fun createApiService(baseUrl: String): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun formatException(e: Exception): String = when (e) {
        is ConnectException, is UnknownHostException -> "无法连接到服务器，请确保电脑服务已启动且与手机在同一WiFi下"
        is SocketTimeoutException -> "连接超时，请检查网络状态"
        else -> e.message ?: "未知错误"
    }

    suspend fun runDiagnostics(ip: String, port: Int): List<DiagnosticsStep> {
        val steps = mutableListOf<DiagnosticsStep>()

        steps.add(runCatching {
            if (ip.isBlank() || !ip.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
                throw IllegalArgumentException("IP 地址格式无效: $ip")
            }
            if (port !in 1..65535) {
                throw IllegalArgumentException("端口号无效: $port (需在 1-65535)")
            }
            "地址格式正确"
        }.fold(
            onSuccess = { DiagnosticsStep("1. 地址格式验证", true, it) },
            onFailure = { DiagnosticsStep("1. 地址格式验证", false, it.message ?: "未知错误") }
        ))

        steps.add(runCatching {
            val address = InetAddress.getByName(ip)
            if (address.isReachable(2000)) {
                "主机可达: ${address.hostAddress}"
            } else {
                "DNS 解析成功: ${address.hostAddress} (主机不可达，可能防火墙拦截 ICMP)"
            }
        }.fold(
            onSuccess = { DiagnosticsStep("2. DNS 解析", true, it) },
            onFailure = { DiagnosticsStep("2. DNS 解析", false, it.message ?: "无法解析主机名") }
        ))

        steps.add(runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 3000)
                "端口 $port 已开放"
            }
        }.fold(
            onSuccess = { DiagnosticsStep("3. TCP 端口连通性", true, it) },
            onFailure = { DiagnosticsStep("3. TCP 端口连通性", false, "端口 $port 不可达 (${it.message})") }
        ))

        steps.add(runCatching {
            val api = createApiService("http://$ip:$port/")
            val response = api.ping()
            if (response.status == "ok") "服务器响应正常: status=ok"
            else "服务器返回异常状态: ${response.status}"
        }.fold(
            onSuccess = { DiagnosticsStep("4. HTTP Ping 请求", true, it) },
            onFailure = { DiagnosticsStep("4. HTTP Ping 请求", false, it.message ?: "HTTP 请求失败") }
        ))

        return steps
    }

    suspend fun ping(ip: String, port: Int): String {
        return try {
            val api = createApiService("http://$ip:$port/")
            val response = api.ping()
            if (response.status == "ok") "ok"
            else "服务器返回异常状态: ${response.status}"
        } catch (e: Exception) {
            formatException(e)
        }
    }

    suspend fun fullSync(
        ip: String,
        port: Int,
        expenses: List<ExpenseRecord>,
        positions: List<FinancePosition>,
        onProgress: (String) -> Unit
    ): SyncResult {
        val api = createApiService("http://$ip:$port/")

        try {
            onProgress("正在同步支出记录...")
            val expenseResponse = api.syncExpenses(SyncExpensesRequest(expenses))
            if (!expenseResponse.success) {
                return SyncResult(0, 0, false, "支出记录同步失败: ${expenseResponse.message}")
            }

            onProgress("正在同步理财记录...")
            val positionResponse = api.syncFinance(SyncFinanceRequest(positions))
            if (!positionResponse.success) {
                return SyncResult(extractCount(expenseResponse.message), 0, false, "理财记录同步失败: ${positionResponse.message}")
            }

            syncPrefs.lastSyncTime = System.currentTimeMillis()
            return SyncResult(
                expenseCount = extractCount(expenseResponse.message),
                positionCount = extractCount(positionResponse.message),
                success = true,
                message = "成功同步 ${extractCount(expenseResponse.message)} 条支出记录，${extractCount(expenseResponse.message)} 条理财记录"
            )
        } catch (e: Exception) {
            return SyncResult(0, 0, false, formatException(e))
        }
    }

    suspend fun incrementalSync(
        ip: String,
        port: Int,
        expenses: List<ExpenseRecord>,
        positions: List<FinancePosition>,
        onProgress: (String) -> Unit
    ): SyncResult {
        val lastSync = syncPrefs.lastSyncTime

        val newExpenses = if (lastSync > 0L) {
            expenses.filter { it.recordedAt > lastSync || (it.id > 0 && it.recordedAt == lastSync) }
        } else {
            expenses
        }

        val newPositions = if (lastSync > 0L) {
            positions.filter { it.updatedAt > lastSync || (it.id > 0 && it.updatedAt == lastSync) }
        } else {
            positions
        }

        if (newExpenses.isEmpty() && newPositions.isEmpty()) {
            return SyncResult(0, 0, true, "没有新的数据需要同步")
        }

        val api = createApiService("http://$ip:$port/")

        try {
            onProgress("正在同步支出记录...")
            val expenseResponse = api.syncExpenses(SyncExpensesRequest(newExpenses))
            if (!expenseResponse.success) {
                return SyncResult(0, 0, false, "支出记录同步失败: ${expenseResponse.message}")
            }

            onProgress("正在同步理财记录...")
            val positionResponse = api.syncFinance(SyncFinanceRequest(newPositions))
            if (!positionResponse.success) {
                return SyncResult(extractCount(expenseResponse.message), 0, false, "理财记录同步失败: ${positionResponse.message}")
            }

            syncPrefs.lastSyncTime = System.currentTimeMillis()
            return SyncResult(
                expenseCount = extractCount(expenseResponse.message),
                positionCount = extractCount(positionResponse.message),
                success = true,
                message = "成功同步 ${extractCount(expenseResponse.message)} 条支出记录，${extractCount(positionResponse.message)} 条理财记录"
            )
        } catch (e: Exception) {
            return SyncResult(0, 0, false, formatException(e))
        }
    }

    private fun extractCount(message: String): Int {
        return try {
            val regex = Regex("""已同步 (\d+)""")
            regex.find(message)?.groupValues?.get(1)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}