package com.example.autobookkeeper.data.entity

data class FinancePosition(
    val id: Int = 0,
    val productName: String = "",
    val platform: String = "",
    val buyAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val profit: Double = 0.0,
    val profitRate: Double = 0.0,
    val screenshotPath: String = "",
    val updatedAt: Long = 0L
)
