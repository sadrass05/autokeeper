package com.example.autobookkeeper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String = "未知商户",
    val platform: String,
    val paymentChannel: String = "未知",
    val category: String,
    val isFinanceExpense: Boolean = false,
    val recordedAt: Long,
    var notificationId: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)