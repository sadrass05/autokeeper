package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.CategoryStat
import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.dao.PlatformStat
import com.example.autobookkeeper.data.entity.ExpenseRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 单元测试 ExpenseRepository.insertRaw 的 dedup 逻辑。
 *
 * 重点覆盖：
 * 1. 同 notificationId 但 amount 不同 → Inserted（防 8.38 vs 8.39 这种浮点容差误判）
 * 2. 同 notificationId + 同 amount → Duplicate
 * 3. 同 notificationId + 同 amount + 更完整字段 → Updated
 */
class ExpenseRepositoryTest {

    private lateinit var fakeDao: FakeExpenseDao
    private lateinit var repo: ExpenseRepository

    @Before
    fun setUp() {
        fakeDao = FakeExpenseDao()
        repo = ExpenseRepository(fakeDao)
    }

    @Test
    fun `different amount with same notificationId should be inserted as new`() = runBlocking {
        fakeDao.seed(
            expense(
                id = 1, notificationId = "nls_X", amount = 8.39,
                merchant = "未知商户", paymentChannel = "微信", category = ""
            )
        )
        val new = expense(
            id = 0, notificationId = "nls_X", amount = 8.38,
            merchant = "未知商户", paymentChannel = "微信", category = "",
            recordedAt = 2000L
        )
        val result = repo.insertRaw(new)
        assertTrue(
            "8.38 should not dedup against 8.39, got: $result",
            result is InsertResult.Inserted
        )
    }

    @Test
    fun `same amount with same notificationId should be duplicate`() = runBlocking {
        fakeDao.seed(
            expense(
                id = 1, notificationId = "nls_X", amount = 8.39,
                merchant = "未知商户", paymentChannel = "微信", category = ""
            )
        )
        val same = expense(
            id = 0, notificationId = "nls_X", amount = 8.39,
            merchant = "未知商户", paymentChannel = "微信", category = "",
            recordedAt = 2000L
        )
        val result = repo.insertRaw(same)
        assertTrue("Same amount should dedup, got: $result", result is InsertResult.Duplicate)
    }

    @Test
    fun `same amount with better merchant should update`() = runBlocking {
        fakeDao.seed(
            expense(
                id = 1, notificationId = "nls_X", amount = 8.39,
                merchant = "未知商户", paymentChannel = "未知", category = ""
            )
        )
        val better = expense(
            id = 0, notificationId = "nls_X", amount = 8.39,
            merchant = "拼多多", paymentChannel = "微信", category = "餐饮",
            recordedAt = 2000L
        )
        val result = repo.insertRaw(better)
        assertTrue("Same amount + better fields should update, got: $result", result is InsertResult.Updated)
        result as InsertResult.Updated
        assertEquals(
            listOf("category", "merchant", "paymentChannel"),
            result.updatedFields.sorted()
        )
    }

    @Test
    fun `same amount outside time window should be inserted as new`() = runBlocking {
        // 已有 8.39 在 3 天前
        fakeDao.seed(
            expense(
                id = 1, notificationId = "nls_X", amount = 8.39,
                merchant = "未知商户", paymentChannel = "微信", category = "",
                recordedAt = 1_000_000L
            )
        )
        // 同一金额 8.39, 但 3 天后 (远超 2 分钟窗口)
        val new = expense(
            id = 0, notificationId = "nls_X", amount = 8.39,
            merchant = "未知商户", paymentChannel = "微信", category = "",
            recordedAt = 1_000_000L + 3L * 24 * 60 * 60 * 1000
        )
        val result = repo.insertRaw(new)
        assertTrue(
            "Same amount but 3 days later should be a new transaction, got: $result",
            result is InsertResult.Inserted
        )
    }

    @Test
    fun `same amount within time window should be duplicate`() = runBlocking {
        // 已有 8.39 在 30 秒前 (2 分钟窗口内)
        fakeDao.seed(
            expense(
                id = 1, notificationId = "nls_X", amount = 8.39,
                merchant = "未知商户", paymentChannel = "微信", category = "",
                recordedAt = 1_000_000L
            )
        )
        // 同一金额 8.39, 30 秒后 (重扫同一通知)
        val rescan = expense(
            id = 0, notificationId = "nls_X", amount = 8.39,
            merchant = "未知商户", paymentChannel = "微信", category = "",
            recordedAt = 1_000_000L + 30_000L
        )
        val result = repo.insertRaw(rescan)
        assertTrue(
            "Rescan within 30s should dedup, got: $result",
            result is InsertResult.Duplicate
        )
    }

    // --- helpers ---

    private fun expense(
        id: Long,
        notificationId: String,
        amount: Double,
        merchant: String,
        paymentChannel: String,
        category: String,
        recordedAt: Long = 1000L
    ) = ExpenseRecord(
        id = id,
        notificationId = notificationId,
        merchant = merchant,
        platform = "微信",
        paymentChannel = paymentChannel,
        category = category,
        amount = amount,
        recordedAt = recordedAt,
        isDeleted = false
    )
}

/**
 * 假 DAO：只实现测试用到的方法。其余方法抛 NotImplementedError。
 *
 * 用 `throw` 表达式 (类型 Nothing) 让编译器推断非空返回类型。
 */
private class FakeExpenseDao : ExpenseDao {
    private val store = mutableListOf<ExpenseRecord>()
    private var nextId = 1000L

    fun seed(record: ExpenseRecord) { store.add(record) }

    override suspend fun findByNotificationId(notificationId: String): List<ExpenseRecord> =
        store.filter { it.notificationId == notificationId && !it.isDeleted }

    override suspend fun insert(record: ExpenseRecord): Long {
        val withId = if (record.id == 0L) record.copy(id = nextId++) else record
        store.add(withId)
        return withId.id
    }

    override suspend fun update(record: ExpenseRecord) {
        val idx = store.indexOfFirst { it.id == record.id }
        if (idx >= 0) store[idx] = record
    }

    override suspend fun existsByNotificationId(notificationId: String): Int =
        store.count { it.notificationId == notificationId && !it.isDeleted }

    // --- 未用方法：抛 NotImplementedError ---
    private fun notImpl(): Nothing = throw NotImplementedError("not needed for this test")

    override fun getAllExpenses(): Flow<List<ExpenseRecord>> = emptyFlow()
    override fun getExpensesByTimeRange(startTime: Long, endTime: Long): Flow<List<ExpenseRecord>> = emptyFlow()
    override fun getExpensesByPlatform(platform: String): Flow<List<ExpenseRecord>> = emptyFlow()
    override fun getExpensesByFinanceFlag(isFinance: Boolean): Flow<List<ExpenseRecord>> = emptyFlow()
    override suspend fun getTotalNonFinanceExpense(): Double? = notImpl()
    override suspend fun getTotalFinanceExpense(): Double? = notImpl()
    override suspend fun getTotalExpenseByMonth(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getNonFinanceExpenseByMonth(startTime: Long, endTime: Long): Double = notImpl()
    override suspend fun getTotalExpenseByMonthIncludingFinance(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getTotalExpenseByDay(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getExpenseForDay(dayStart: Long, dayEnd: Long): Double? = notImpl()
    override suspend fun getExpenseByCategory(): List<CategoryStat> = notImpl()
    override suspend fun getTodayExpenseByCategory(dayStart: Long, dayEnd: Long): List<CategoryStat> = notImpl()
    override suspend fun getExpenseByPlatform(): List<PlatformStat> = notImpl()
    override suspend fun insertAll(expenses: List<ExpenseRecord>): Unit = notImpl()
    override suspend fun delete(expense: ExpenseRecord): Unit = notImpl()
    override suspend fun deleteById(id: Long): Unit = notImpl()
    override suspend fun softDelete(id: Long): Unit = notImpl()
    override suspend fun markAsDeleted(id: Long, timestamp: Long): Unit = notImpl()
    override fun getExpensesBeforeTime(time: Long): Flow<List<ExpenseRecord>> = emptyFlow()
    override fun getAllActiveExpenses(): Flow<List<ExpenseRecord>> = emptyFlow()
    override fun getDeletedExpenses(): Flow<List<ExpenseRecord>> = emptyFlow()
    override suspend fun deleteExpired(threshold: Long): Unit = notImpl()
    override suspend fun deleteAllDeleted(): Unit = notImpl()
    override suspend fun restoreExpense(id: Long): Unit = notImpl()
    override suspend fun clearAllFinanceFlags(): Unit = notImpl()
    override suspend fun getAllExpensesList(): List<ExpenseRecord> = notImpl()
    override suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int = notImpl()
    override suspend fun countDuplicates(merchant: String, amount: Double, recordedAt: Long): Int = notImpl()
}
