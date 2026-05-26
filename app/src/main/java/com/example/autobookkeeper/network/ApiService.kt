package com.example.autobookkeeper.network

import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    suspend fun getExpenses(): List<ExpenseRecord>
    suspend fun getFinancePositions(): List<FinancePosition>
    @GET("ping")
    suspend fun ping(): PingResponse

    @POST("sync/expenses")
    suspend fun syncExpenses(@Body request: SyncExpensesRequest): SyncResponse

    @POST("sync/positions")
    suspend fun syncFinance(@Body request: SyncFinanceRequest): SyncResponse
}

data class SyncExpensesRequest(
    val records: List<ExpenseRecord>
)

data class SyncFinanceRequest(
    val positions: List<FinancePosition>
)

data class PingResponse(val status: String)

data class SyncResponse(
    val success: Boolean,
    val message: String
)