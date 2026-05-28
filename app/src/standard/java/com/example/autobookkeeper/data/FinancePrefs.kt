package com.example.autobookkeeper.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    var accumulatedProfit: Double = 0.0
}
