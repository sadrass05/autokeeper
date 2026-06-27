package com.example.autobookkeeper.backup

import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.repository.ExpenseRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 验证 CSV 解析器在 RFC 4180 兼容性:
 * 1. 含逗号字段 (商户名, 分类) 不被切错
 * 2. 含双引号转义 "" 的字段被正确解析
 * 3. 写入 + 读取的 round-trip 数据一致
 */
class BackupCsvParserTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `parses CSV with quoted fields containing commas`() {
        // 准备: 商户名含逗号, 用引号包围
        val csvFile = tmp.newFile("quoted.csv")
        csvFile.writeText(
            "\uFEFF日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出\n" +
                "2024-06-15 12:30,\"星巴克, 国贸店\",38.00,支付宝,支付宝,餐饮,否\n"
        )

        // 执行
        val records = invokeParseCsv(csvFile)

        // 断言: 商户名完整保留逗号
        assertEquals(1, records.size)
        assertEquals("星巴克, 国贸店", records[0].merchant)
        assertEquals(38.00, records[0].amount, 0.001)
        assertEquals("支付宝", records[0].platform)
        assertEquals("餐饮", records[0].category)
    }

    @Test
    fun `parses CSV with escaped double quotes inside fields`() {
        // 准备: 商户名含双引号, 用 "" 转义
        val csvFile = tmp.newFile("escaped.csv")
        csvFile.writeText(
            "\uFEFF日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出\n" +
                "2024-06-15 12:30,\"他说\"\"你好\"\"\",10.00,微信,微信,餐饮,否\n"
        )

        val records = invokeParseCsv(csvFile)

        // 断言: "" 被解析为单个 "
        assertEquals(1, records.size)
        assertEquals("他说\"你好\"", records[0].merchant)
    }

    @Test
    fun `parses multiple rows with mixed special characters`() {
        val csvFile = tmp.newFile("mixed.csv")
        csvFile.writeText(
            "\uFEFF日期时间,商户名称,金额,平台,支付渠道,分类,是否理财支出\n" +
                "2024-06-15 12:30,星巴克,38.00,支付宝,支付宝,餐饮,否\n" +
                "2024-06-15 13:00,\"麦当劳, 北京站\",25.00,微信,微信,餐饮,否\n" +
                "2024-06-15 14:00,\"某公司\"\"XX\"\"分公司\",100.00,银行卡,招商银行,工资,是\n"
        )

        val records = invokeParseCsv(csvFile)

        assertEquals(3, records.size)
        assertEquals("星巴克", records[0].merchant)
        assertEquals("麦当劳, 北京站", records[1].merchant)
        assertEquals("某公司\"XX\"分公司", records[2].merchant)
        assertEquals(true, records[2].isFinanceExpense)
    }

    @Test
    fun `round-trip CSV write then read preserves special characters`() {
        val csvFile = tmp.newFile("rt.csv")
        val mgr = TestableBackupManager(tmp)

        val original = listOf(
            sampleRecord(merchant = "星巴克, 国贸店", amount = 38.0, platform = "支付宝", category = "餐饮"),
            sampleRecord(merchant = "他说\"你好\"", amount = 10.0, platform = "微信", category = "餐饮"),
            sampleRecord(merchant = "普通商户名", amount = 5.0, platform = "银行卡", category = "购物")
        )

        // 写入
        mgr.writeCsvForTest(csvFile, original)
        // 读回
        val readBack = mgr.parseCsvForTest(csvFile)

        // 断言
        assertEquals(original.size, readBack.size)
        assertEquals("星巴克, 国贸店", readBack[0].merchant)
        assertEquals(38.0, readBack[0].amount, 0.001)
        assertEquals("他说\"你好\"", readBack[1].merchant)
        assertEquals("普通商户名", readBack[2].merchant)
        assertEquals("购物", readBack[2].category)
    }

    @Test
    fun `fields without special chars are not quoted in output`() {
        val csvFile = tmp.newFile("noquote.csv")
        val mgr = TestableBackupManager(tmp)
        val original = listOf(
            sampleRecord(merchant = "普通商户", amount = 10.0, platform = "支付宝", category = "餐饮")
        )

        mgr.writeCsvForTest(csvFile, original)
        val text = csvFile.readText()

        // 断言: 普通字段不加引号
        assertEquals(false, text.contains("\"普通商户\""))
        assertEquals(false, text.contains("\"支付宝\""))
    }

    // --- 辅助 ---

    private fun invokeParseCsv(file: File): List<ExpenseRecord> {
        return TestableBackupManager(tmp).parseCsvForTest(file)
    }

    private fun sampleRecord(
        merchant: String,
        amount: Double,
        platform: String,
        category: String,
        isFinance: Boolean = false
    ) = ExpenseRecord(
        merchant = merchant,
        amount = amount,
        platform = platform,
        paymentChannel = platform,
        category = category,
        isFinanceExpense = isFinance,
        recordedAt = 1718435400000L,  // 2024-06-15 12:30
        notificationId = "test-${System.nanoTime()}"
    )
}

/**
 * 测试专用 BackupManager 子类: 复用 secondary constructor 接受 tmpDir,
 * 暴露 parseCsvToExpenses / writeCsv / cleanOldBackupsImpl 给测试.
 */
internal class TestableBackupManager(tmp: TemporaryFolder) {
    private val delegate: BackupManager

    init {
        val fakeDao = StubExpenseDao()
        val fakeRepo = ExpenseRepository(fakeDao)
        delegate = BackupManager(
            context = NoopContextStub,
            expenseRepository = fakeRepo,
            backupDir = tmp.newFolder("backups")
        )
    }

    fun parseCsvForTest(file: File): List<ExpenseRecord> = delegate.parseCsvToExpenses(file)
    fun writeCsvForTest(file: File, expenses: List<ExpenseRecord>) = delegate.writeCsvForTest(file, expenses)
    fun cleanOldForTest(dir: File, keep: Int) = delegate.cleanOldBackupsImplForTest(dir, keep)
}

/** Stub DAO: 永远返回空 expenses, 防止测试路径意外触发数据库读取 */
internal class StubExpenseDao : ExpenseDao {
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

/** 用 ContextWrapper(null) 创建一个空 Context 占位, BackupManager 不会主动调用. */
internal val NoopContextStub: android.content.Context =
    object : android.content.ContextWrapper(null) {}
