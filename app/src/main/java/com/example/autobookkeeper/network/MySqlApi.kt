package com.example.autobookkeeper.network

import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition

class MySqlApi(private val apiService: ApiService) {

    suspend fun syncExpenses(records: List<ExpenseRecord>) {
        try {
            val response = apiService.syncExpenses(SyncExpensesRequest(records))
            if (!response.success) {
                throw RuntimeException(response.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getExpenses(): List<ExpenseRecord> {
        return try {
            apiService.getExpenses()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun syncFinance(positions: List<FinancePosition>) {
        try {
            val response = apiService.syncFinance(SyncFinanceRequest(positions))
            if (!response.success) {
                throw RuntimeException(response.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getFinancePositions(): List<FinancePosition> {
        return try {
            apiService.getFinancePositions()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}