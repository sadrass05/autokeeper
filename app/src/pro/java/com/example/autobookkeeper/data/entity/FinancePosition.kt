package com.example.autobookkeeper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_positions")
data class FinancePosition(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productName: String,
    val platform: String,
    val buyAmount: Double,
    val currentValue: Double,
    val profit: Double,
    val profitRate: Double,
    val screenshotPath: String,
    val updatedAt: Long
)
