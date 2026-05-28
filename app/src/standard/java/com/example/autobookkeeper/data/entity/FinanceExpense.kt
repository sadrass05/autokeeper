package com.example.autobookkeeper.data.entity

data class FinanceExpense(
    val id: Int = 0,
    val amount: Double = 0.0,
    val description: String = "",
    val fromProduct: String = "",
    val recordedAt: Long = 0L
)
