package com.example.autobookkeeper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_expenses")
data class FinanceExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val description: String,
    val fromProduct: String,
    val recordedAt: Long = System.currentTimeMillis()
)
