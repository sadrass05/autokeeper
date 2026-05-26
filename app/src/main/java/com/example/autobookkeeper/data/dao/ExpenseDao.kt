package com.example.autobookkeeper.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.autobookkeeper.data.entity.ExpenseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY recordedAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expenses WHERE recordedAt >= :startTime AND recordedAt <= :endTime AND isDeleted = 0 ORDER BY recordedAt DESC")
    fun getExpensesByTimeRange(startTime: Long, endTime: Long): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expenses WHERE platform = :platform AND isDeleted = 0 ORDER BY recordedAt DESC")
    fun getExpensesByPlatform(platform: String): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expenses WHERE isFinanceExpense = :isFinance AND isDeleted = 0 ORDER BY recordedAt DESC")
    fun getExpensesByFinanceFlag(isFinance: Boolean): Flow<List<ExpenseRecord>>

    @Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0")
    suspend fun getTotalNonFinanceExpense(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = true AND isDeleted = 0")
    suspend fun getTotalFinanceExpense(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 AND recordedAt >= :startTime AND recordedAt <= :endTime")
    suspend fun getTotalExpenseByMonth(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 AND recordedAt >= :startTime AND recordedAt <= :endTime")
    suspend fun getTotalExpenseByDay(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 AND recordedAt >= :dayStart AND recordedAt <= :dayEnd")
    suspend fun getExpenseForDay(dayStart: Long, dayEnd: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 GROUP BY category")
    suspend fun getExpenseByCategory(): List<CategoryStat>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 AND recordedAt >= :dayStart AND recordedAt <= :dayEnd GROUP BY category")
    suspend fun getTodayExpenseByCategory(dayStart: Long, dayEnd: Long): List<CategoryStat>

    @Query("SELECT platform, SUM(amount) as total FROM expenses WHERE isFinanceExpense = false AND isDeleted = 0 GROUP BY platform")
    suspend fun getExpenseByPlatform(): List<PlatformStat>

    @Query("SELECT COUNT(*) FROM expenses WHERE notificationId = :notificationId")
    suspend fun existsByNotificationId(notificationId: String): Int

    @Insert
    suspend fun insert(expense: ExpenseRecord): Long

    @Update
    suspend fun update(expense: ExpenseRecord)

    @Delete
    suspend fun delete(expense: ExpenseRecord)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE expenses SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM expenses WHERE isDeleted = 1 ORDER BY recordedAt DESC")
    fun getDeletedExpenses(): Flow<List<ExpenseRecord>>

    @Query("DELETE FROM expenses WHERE isDeleted = 1 AND recordedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("DELETE FROM expenses WHERE isDeleted = 1")
    suspend fun deleteAllDeleted()

    @Query("UPDATE expenses SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreExpense(id: Long)

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY recordedAt DESC")
    suspend fun getAllExpensesList(): List<ExpenseRecord>

    @Query("""
        DELETE FROM expenses
        WHERE recordedAt >= :startOfDay
        AND recordedAt <= :endOfDay
        AND (notificationId LIKE 'import_%'
             OR notificationId LIKE 'manual_%'
             OR notificationId = '')
        AND isDeleted = 0
    """)
    suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int
}

data class CategoryStat(val category: String, val total: Double)
data class PlatformStat(val platform: String, val total: Double)