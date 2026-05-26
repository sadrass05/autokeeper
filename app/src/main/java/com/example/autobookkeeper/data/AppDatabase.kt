package com.example.autobookkeeper.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.dao.FinanceDao
import com.example.autobookkeeper.data.dao.FinanceExpenseDao
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinanceExpense
import com.example.autobookkeeper.data.entity.FinancePosition

@Database(
    entities = [ExpenseRecord::class, FinancePosition::class, FinanceExpense::class, Category::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun financeDao(): FinanceDao
    abstract fun financeExpenseDao(): FinanceExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(expenses)")
                var hasIsDeletedUnderscore = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    if (cursor.getString(nameIndex) == "is_deleted") {
                        hasIsDeletedUnderscore = true
                        break
                    }
                }
                cursor.close()
                if (hasIsDeletedUnderscore) {
                    db.execSQL("ALTER TABLE expenses RENAME COLUMN is_deleted TO isDeleted")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS finance_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        fromProduct TEXT NOT NULL,
                        recordedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}