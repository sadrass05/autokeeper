package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.entity.ExpenseRecord
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

    suspend fun getNonFinanceExpenseByMonth(startTime: Long, endTime: Long): Double {
        return expenseDao.getNonFinanceExpenseByMonth(startTime, endTime)
    }

    suspend fun getTotalExpenseByMonthIncludingFinance(start: Long, end: Long): Double {
        return expenseDao.getTotalExpenseByMonthIncludingFinance(start, end) ?: 0.0
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

    suspend fun findByNotificationId(notificationId: String): List<ExpenseRecord> {
        return expenseDao.findByNotificationId(notificationId)
    }

    suspend fun insertExpense(expense: ExpenseRecord) {
        if (!existsByNotificationId(expense.notificationId)) {
            expenseDao.insert(expense)
        }
    }

    /**
     * 插入一条原始通知解析结果。
     *
     * - 首次遇到该 notificationId → 插入，返回 [InsertResult.Inserted]
     * - notificationId 已存在但 amount 不匹配（WeChat 复用 notificationId 的碰撞）→ 视为新交易，插入新记录
     * - notificationId 已存在且 amount 匹配（同笔交易重扫）→ 新解析有更完整字段则 update，否则 [InsertResult.Duplicate]
     */
    suspend fun insertRaw(expense: ExpenseRecord): InsertResult {
        val candidates = expenseDao.findByNotificationId(expense.notificationId)
        if (candidates.isEmpty()) {
            expenseDao.insert(expense)
            return InsertResult.Inserted
        }

        val sameAmount = candidates.firstOrNull {
            it.amount == expense.amount &&
                kotlin.math.abs(it.recordedAt - expense.recordedAt) < TIME_WINDOW_MS
        }
        if (sameAmount == null) {
            android.util.Log.w(
                "AutoBookkeeper",
                "⚠️ notificationId 碰撞: 视为新交易 notificationId=${expense.notificationId} " +
                    "新 amount=${expense.amount} at=${expense.recordedAt} " +
                    "已有 ${candidates.map { "${it.amount}(${it.recordedAt} => ${(expense.recordedAt - it.recordedAt) / 1000}s ago)" }}"
            )
            expenseDao.insert(expense)
            return InsertResult.Inserted
        }

        val newMerchant = expense.merchant.takeIf { it.isNotBlank() && it != "未知商户" }
        val newChannel = expense.paymentChannel.takeIf { it.isNotBlank() && it != "未知" }
        val newCategory = expense.category.takeIf { it.isNotBlank() }

        val merchantChanged = newMerchant != null && sameAmount.merchant != newMerchant
        val channelChanged = newChannel != null && sameAmount.paymentChannel != newChannel
        val categoryChanged = newCategory != null && sameAmount.category.isBlank()

        if (!merchantChanged && !channelChanged && !categoryChanged) {
            return InsertResult.Duplicate(sameAmount)
        }

        val updated = sameAmount.copy(
            merchant = if (merchantChanged) newMerchant!! else sameAmount.merchant,
            paymentChannel = if (channelChanged) newChannel!! else sameAmount.paymentChannel,
            category = if (categoryChanged) newCategory!! else sameAmount.category
        )
        expenseDao.update(updated)
        return InsertResult.Updated(
            existing = sameAmount,
            updatedFields = buildList {
                if (merchantChanged) add("merchant")
                if (channelChanged) add("paymentChannel")
                if (categoryChanged) add("category")
            }
        )
    }

    suspend fun insertExpenseWithDedup(expense: ExpenseRecord): Boolean {
        if (existsByNotificationId(expense.notificationId)) {
            return false
        }
        val duplicateCount = expenseDao.countDuplicates(expense.merchant, expense.amount, expense.recordedAt)
        if (duplicateCount > 0) {
            return false
        }
        expenseDao.insert(expense)
        return true
    }

    suspend fun insertExpensesBatch(expenses: List<ExpenseRecord>): Int {
        val newRecords = expenses.filter { !existsByNotificationId(it.notificationId) }
        if (newRecords.isNotEmpty()) {
            expenseDao.insertAll(newRecords)
        }
        return newRecords.size
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
        expenseDao.markAsDeleted(id, System.currentTimeMillis())
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

    fun getExpensesBeforeTime(time: Long): Flow<List<ExpenseRecord>> {
        return expenseDao.getExpensesBeforeTime(time)
    }

    companion object {
        /**
         * Dedup 时间窗口：同 [ExpenseRecord.notificationId] + 同 amount + 两次 parse 的
         * [ExpenseRecord.recordedAt] 差值在窗口内 → 视为同一笔（重扫 / 通知更新）。
         * 超过窗口 → 视为新交易（WeChat 复用 notificationId 跨多笔交易）。
         */
        private const val TIME_WINDOW_MS: Long = 2 * 60 * 1000L
    }
}

/**
 * [ExpenseRepository.insertRaw] 的返回结果。
 */
sealed class InsertResult {
    /** 全新插入。 */
    data object Inserted : InsertResult()

    /**
     * 已存在重复，但新解析包含更完整信息，已就地更新。
     *
     * @property existing 更新前的记录
     * @property updatedFields 被改写的字段名（merchant / paymentChannel / category）
     */
    data class Updated(
        val existing: ExpenseRecord,
        val updatedFields: List<String>
    ) : InsertResult()

    /**
     * 已存在重复且新解析没有更多有效信息，跳过。
     *
     * @property existing 当前 DB 中已存在的记录
     */
    data class Duplicate(val existing: ExpenseRecord) : InsertResult()
}