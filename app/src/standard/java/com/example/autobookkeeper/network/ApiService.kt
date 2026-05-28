package com.example.autobookkeeper.network

import com.example.autobookkeeper.data.entity.ExpenseRecord
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    suspend fun getExpenses(): List<ExpenseRecord>
    @GET("ping")
    suspend fun ping(): PingResponse

    @POST("sync/expenses")
    suspend fun syncExpenses(@Body request: SyncExpensesRequest): SyncResponse
}

data class SyncExpensesRequest(
    val records: List<ExpenseRecord>
)

data class PingResponse(val status: String)

data class SyncResponse(
    val success: Boolean,
    val message: String
)