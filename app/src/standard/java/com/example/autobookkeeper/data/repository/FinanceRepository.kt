package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.entity.FinancePosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FinanceRepository @Inject constructor() {

    fun getAllPositions(): Flow<List<FinancePosition>> = flowOf(emptyList())

    fun getPositionsByPlatform(platform: String): Flow<List<FinancePosition>> = flowOf(emptyList())

    suspend fun getTotalProfit(): Double = 0.0

    suspend fun getTotalCurrentValue(): Double = 0.0

    suspend fun getTotalBuyAmount(): Double = 0.0

    suspend fun insertPosition(position: FinancePosition): Long = 0

    suspend fun updatePosition(position: FinancePosition) {}

    suspend fun deletePosition(position: FinancePosition) {}

    suspend fun deletePositionById(id: Long) {}

    suspend fun deleteByIds(ids: List<Long>) {}

    suspend fun insertPositions(positions: List<FinancePosition>) {}

    suspend fun upsertPosition(position: FinancePosition) {}

    suspend fun getAllPositionsOnce(): List<FinancePosition> = emptyList()
}
