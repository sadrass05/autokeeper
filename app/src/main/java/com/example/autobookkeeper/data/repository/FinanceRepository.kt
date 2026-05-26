package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.FinanceDao
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.FinancePosition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FinanceRepository @Inject constructor(
    private val financeDao: FinanceDao
) {

    fun getAllPositions(): Flow<List<FinancePosition>> {
        return financeDao.getAllPositions()
    }

    fun getPositionsByPlatform(platform: String): Flow<List<FinancePosition>> {
        return financeDao.getPositionsByPlatform(platform)
    }

    suspend fun getTotalProfit(): Double {
        return financeDao.getTotalProfit() ?: 0.0
    }

    suspend fun getTotalCurrentValue(): Double {
        return financeDao.getTotalCurrentValue() ?: 0.0
    }

    suspend fun getTotalBuyAmount(): Double {
        return financeDao.getTotalBuyAmount() ?: 0.0
    }

    suspend fun insertPosition(position: FinancePosition): Long {
        return financeDao.insert(position)
    }

    suspend fun updatePosition(position: FinancePosition) {
        financeDao.update(position)
    }

    suspend fun deletePosition(position: FinancePosition) {
        financeDao.delete(position)
    }

    suspend fun deletePositionById(id: Long) {
        financeDao.deleteById(id)
    }

    suspend fun deleteByIds(ids: List<Long>) {
        financeDao.deleteByIds(ids)
    }

    suspend fun insertPositions(positions: List<FinancePosition>) {
        financeDao.insertPositions(positions)
    }

    suspend fun upsertPosition(position: FinancePosition) {
        val existing = financeDao.getByName(position.productName)
        if (existing != null) {
            val updated = existing.copy(
                buyAmount = existing.buyAmount + position.buyAmount,
                currentValue = position.currentValue,
                profit = position.profit,
                profitRate = position.profitRate,
                platform = position.platform.ifEmpty { existing.platform },
                screenshotPath = position.screenshotPath.ifEmpty { existing.screenshotPath },
                updatedAt = System.currentTimeMillis()
            )
            financeDao.update(updated)
        } else {
            financeDao.insert(position)
        }
    }

    suspend fun getAllPositionsOnce(): List<FinancePosition> {
        return financeDao.getAllPositionsList()
    }
}