package com.example.autobookkeeper.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.autobookkeeper.data.entity.FinanceExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceExpenseDao {
    @Insert
    suspend fun insert(expense: FinanceExpense)

    @Delete
    suspend fun delete(expense: FinanceExpense)

    @Query("SELECT * FROM finance_expenses ORDER BY recordedAt DESC")
    fun getAllExpenses(): Flow<List<FinanceExpense>>

    @Query("SELECT SUM(amount) FROM finance_expenses")
    suspend fun getTotalExpenses(): Double?
}
