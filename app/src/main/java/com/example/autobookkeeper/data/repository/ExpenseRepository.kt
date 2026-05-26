package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.dao.FinanceDao
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    fun getAllExpenses(): Flow<List<ExpenseRecord>> {
        return expenseDao.getAllExpenses()
    }

    fun getExpensesByTimeRange(startTime: Long, endTime: Long): Flow<List<ExpenseRecord>> {
        return expenseDao.getExpensesByTimeRange(startTime, endTime)
    }

    fun getExpensesByPlatform(platform: String): Flow<List<ExpenseRecord>> {
        return expenseDao.getExpensesByPlatform(platform)
    }

    suspend fun getTotalNonFinanceExpense(): Double {
        return expenseDao.getTotalNonFinanceExpense() ?: 0.0
    }

    suspend fun getTotalFinanceExpense(): Double {
        return expenseDao.getTotalFinanceExpense() ?: 0.0
    }

    suspend fun getTotalExpenseByMonth(startTime: Long, endTime: Long): Double {
        return expenseDao.getTotalExpenseByMonth(startTime, endTime) ?: 0.0
    }

    suspend fun getTotalExpenseByDay(startTime: Long, endTime: Long): Double? {
        return expenseDao.getTotalExpenseByDay(startTime, endTime)
    }

    suspend fun getExpenseForDay(dayStart: Long, dayEnd: Long): Double {
        return expenseDao.getExpenseForDay(dayStart, dayEnd) ?: 0.0
    }

    suspend fun getExpenseByCategory(): List<Pair<String, Double>> {
        return expenseDao.getExpenseByCategory().map { it.category to it.total }
    }

    suspend fun getTodayExpenseByCategory(dayStart: Long, dayEnd: Long): List<Pair<String, Double>> {
        return expenseDao.getTodayExpenseByCategory(dayStart, dayEnd).map { it.category to it.total }
    }

    suspend fun getExpenseByPlatform(): List<Pair<String, Double>> {
        return expenseDao.getExpenseByPlatform().map { it.platform to it.total }
    }

    suspend fun existsByNotificationId(notificationId: String): Boolean {
        return expenseDao.existsByNotificationId(notificationId) > 0
    }

    suspend fun insertExpense(expense: ExpenseRecord) {
        if (!existsByNotificationId(expense.notificationId)) {
            expenseDao.insert(expense)
        }
    }

    suspend fun updateExpense(expense: ExpenseRecord) {
        expenseDao.update(expense)
    }

    suspend fun deleteExpense(expense: ExpenseRecord) {
        expenseDao.delete(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteById(id)
    }

    suspend fun softDelete(id: Long) {
        expenseDao.softDelete(id)
    }

    suspend fun restoreExpense(id: Long) {
        expenseDao.restoreExpense(id)
    }

    suspend fun deleteAllDeleted() {
        expenseDao.deleteAllDeleted()
    }

    suspend fun deleteExpired(threshold: Long) {
        expenseDao.deleteExpired(threshold)
    }

    fun getDeletedExpenses(): Flow<List<ExpenseRecord>> {
        return expenseDao.getDeletedExpenses()
    }

    suspend fun getAllExpensesOnce(): List<ExpenseRecord> {
        return expenseDao.getAllExpensesList()
    }

    suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int {
        return expenseDao.deleteImportedDataOnDay(startOfDay, endOfDay)
    }

    fun getFinanceFlaggedExpenses(): Flow<List<ExpenseRecord>> {
        return expenseDao.getExpensesByFinanceFlag(true)
    }

    suspend fun getTotalFinanceFlaggedExpense(): Double {
        return expenseDao.getTotalFinanceExpense() ?: 0.0
    }
}