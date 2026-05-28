package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.entity.FinanceExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FinanceExpenseRepository @Inject constructor() {

    fun getAllExpenses(): Flow<List<FinanceExpense>> = flowOf(emptyList())

    suspend fun insert(expense: FinanceExpense) {}

    suspend fun delete(expense: FinanceExpense) {}

    suspend fun getTotalExpenses(): Double = 0.0
}
