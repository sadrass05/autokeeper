package com.example.autobookkeeper.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 验证 cleanOldBackupsImpl 按数量保留最近 N 个文件.
 * 修复前用 14 天硬编码窗口 + 7 天周期, 实际可能只保留 1 个备份.
 * 修复后按数量保留 (默认 4 个), 与备份周期解耦.
 */
class BackupCleanerTest {

    @get:org.junit.Rule
    val tmp = TemporaryFolder()

    @Test
    fun `cleanOldBackupsImpl keeps only the N most recent backup files`() {
        val backupDir = tmp.newFolder("backups")
        val mgr = TestableBackupManager(tmp)

        // 准备: 创建 6 个备份文件, 名称带日期序号
        val now = System.currentTimeMillis()
        val created = (1..6).map { i ->
            java.io.File(backupDir, "backup_2024-06-1${i}.csv").apply {
                writeText("dummy")
                setLastModified(now - (6 - i) * 86_400_000L)  // 1 6天前, 6 今日
            }
        }
        assertEquals(6, backupDir.listFiles()!!.size)

        // 执行
        mgr.cleanOldForTest(backupDir, keep = 4)

        // 断言: 保留最近 4 个 (4, 5, 6 即 06-12, 06-13, 06-14, 06-15)
        val remaining = backupDir.listFiles()!!.sortedBy { it.lastModified() }
        assertEquals(4, remaining.size)
        assertEquals("backup_2024-06-12.csv", remaining[0].name)
        assertEquals("backup_2024-06-15.csv", remaining[3].name)

        // 验证 1, 2, 3 已被删除
        val remainingNames = remaining.map { it.name }
        assertTrue("backup_2024-06-11.csv 应被删除", "backup_2024-06-11.csv" !in remainingNames)
        assertTrue("backup_2024-06-10.csv 应被删除", "backup_2024-06-10.csv" !in remainingNames)
    }

    @Test
    fun `cleanOldBackupsImpl does nothing when file count is below keep`() {
        val backupDir = tmp.newFolder("backups")
        val mgr = TestableBackupManager(tmp)

        // 准备: 2 个文件, 少于 keep=4
        java.io.File(backupDir, "backup_a.csv").writeText("a")
        java.io.File(backupDir, "backup_b.csv").writeText("b")
        assertEquals(2, backupDir.listFiles()!!.size)

        // 执行
        mgr.cleanOldForTest(backupDir, keep = 4)

        // 断言: 都不删
        assertEquals(2, backupDir.listFiles()!!.size)
    }

    @Test
    fun `cleanOldBackupsImpl ignores non-CSV files in directory`() {
        val backupDir = tmp.newFolder("backups")
        val mgr = TestableBackupManager(tmp)

        // 准备: 5 个 csv + 1 个 txt (应被忽略)
        repeat(5) { i ->
            java.io.File(backupDir, "backup_$i.csv").apply {
                writeText("c")
                setLastModified(System.currentTimeMillis() - (5 - i) * 1000L)
            }
        }
        java.io.File(backupDir, "README.txt").writeText("note")
        assertEquals(6, backupDir.listFiles()!!.size)

        // 执行
        mgr.cleanOldForTest(backupDir, keep = 3)

        // 断言: csv 留 3 个, txt 不动
        val csvs = backupDir.listFiles()!!.filter { it.name.endsWith(".csv") }
        assertEquals(3, csvs.size)
        assertTrue(java.io.File(backupDir, "README.txt").exists())
    }
}
