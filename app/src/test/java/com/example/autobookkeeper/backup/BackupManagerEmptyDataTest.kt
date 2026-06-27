package com.example.autobookkeeper.backup

import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.repository.ExpenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 验证空数据库时备份返回 Success(count=0), 不再返回 Failure.
 *
 * 修复前: BackupManager.performWeeklyBackup() 遇到空 expenses 返回
 *         BackupResult.Failure("无数据可备份"), Worker 视作永久失败, 首次安装/新用户永远没备份.
 * 修复后: 视为 Success(fileName="", count=0), Worker 视作成功, 符合用户预期.
 */
class BackupManagerEmptyDataTest {

    @get:org.junit.Rule
    val tmp = TemporaryFolder()

    @Test
    fun `performWeeklyBackup returns Success when database is empty`() = runBlocking {
        // 准备: 假 DAO 永远返回空列表; 用 secondary constructor 注入 tmpDir
        val mgr = BackupManager(
            context = NoopContextStub,
            expenseRepository = ExpenseRepository(EmptyExpenseDao),
            backupDir = tmp.newFolder("backups")
        )

        // 执行
        val result = mgr.performWeeklyBackup()

        // 断言: 空数据视为成功, count=0
        assertTrue("空数据库应该返回 Success, 实际: $result", result is BackupResult.Success)
        result as BackupResult.Success
        assertEquals(0, result.count)
        assertEquals("", result.fileName)
    }
}

/** 最小化桩 DAO: 只实现 getAllExpensesList, 其他方法抛 NotImplementedError */
internal object EmptyExpenseDao : com.example.autobookkeeper.data.dao.ExpenseDao {
    override suspend fun getAllExpensesList(): List<ExpenseRecord> = emptyList()

    private fun notImpl(): Nothing = throw NotImplementedError("not used in this test")

    override fun getAllExpenses() = notImpl()
    override fun getExpensesByTimeRange(startTime: Long, endTime: Long) = notImpl()
    override fun getExpensesByPlatform(platform: String) = notImpl()
    override fun getExpensesByFinanceFlag(isFinance: Boolean) = notImpl()
    override suspend fun getTotalNonFinanceExpense(): Double? = notImpl()
    override suspend fun getTotalFinanceExpense(): Double? = notImpl()
    override suspend fun getTotalExpenseByMonth(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getNonFinanceExpenseByMonth(startTime: Long, endTime: Long): Double = notImpl()
    override suspend fun getTotalExpenseByMonthIncludingFinance(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getTotalExpenseByDay(startTime: Long, endTime: Long): Double? = notImpl()
    override suspend fun getExpenseForDay(dayStart: Long, dayEnd: Long): Double? = notImpl()
    override suspend fun getExpenseByCategory() = notImpl()
    override suspend fun getTodayExpenseByCategory(dayStart: Long, dayEnd: Long) = notImpl()
    override suspend fun getExpenseByPlatform() = notImpl()
    override suspend fun existsByNotificationId(notificationId: String): Int = notImpl()
    override suspend fun findByNotificationId(notificationId: String): List<ExpenseRecord> = notImpl()
    override suspend fun insert(record: ExpenseRecord): Long = notImpl()
    override suspend fun insertAll(expenses: List<ExpenseRecord>) = notImpl()
    override suspend fun update(record: ExpenseRecord) = notImpl()
    override suspend fun delete(record: ExpenseRecord) = notImpl()
    override suspend fun deleteById(id: Long) = notImpl()
    override suspend fun softDelete(id: Long) = notImpl()
    override suspend fun markAsDeleted(id: Long, timestamp: Long) = notImpl()
    override fun getExpensesBeforeTime(time: Long) = notImpl()
    override fun getAllActiveExpenses() = notImpl()
    override fun getDeletedExpenses() = notImpl()
    override suspend fun deleteExpired(threshold: Long) = notImpl()
    override suspend fun deleteAllDeleted() = notImpl()
    override suspend fun restoreExpense(id: Long) = notImpl()
    override suspend fun clearAllFinanceFlags() = notImpl()
    override suspend fun deleteImportedDataOnDay(startOfDay: Long, endOfDay: Long): Int = notImpl()
    override suspend fun countDuplicates(merchant: String, amount: Double, recordedAt: Long): Int = notImpl()
}
