package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.FinanceExpenseDao
import com.example.autobookkeeper.data.entity.FinanceExpense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FinanceExpenseRepository @Inject constructor(
    private val dao: FinanceExpenseDao
) {
    fun getAllExpenses(): Flow<List<FinanceExpense>> = dao.getAllExpenses()

    suspend fun insert(expense: FinanceExpense) {
        dao.insert(expense)
    }

    suspend fun delete(expense: FinanceExpense) {
        dao.delete(expense)
    }

    suspend fun getTotalExpenses(): Double {
        return dao.getTotalExpenses() ?: 0.0
    }
}