package com.example.autobookkeeper.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE)

    var accumulatedProfit: Double
        get() = prefs.getFloat("accumulated_profit", 0f).toDouble()
        set(value) = prefs.edit().putFloat("accumulated_profit", value.toFloat()).apply()
}