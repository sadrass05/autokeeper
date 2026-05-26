package com.example.autobookkeeper.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.autobookkeeper.data.entity.FinancePosition
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    @Query("SELECT * FROM finance_positions ORDER BY updatedAt DESC")
    fun getAllPositions(): Flow<List<FinancePosition>>

    @Query("SELECT * FROM finance_positions WHERE platform = :platform ORDER BY updatedAt DESC")
    fun getPositionsByPlatform(platform: String): Flow<List<FinancePosition>>

    @Query("SELECT SUM(profit) FROM finance_positions")
    suspend fun getTotalProfit(): Double?

    @Query("SELECT SUM(currentValue) FROM finance_positions")
    suspend fun getTotalCurrentValue(): Double?

    @Query("SELECT SUM(buyAmount) FROM finance_positions")
    suspend fun getTotalBuyAmount(): Double?

    @Insert
    suspend fun insert(position: FinancePosition): Long

    @Update
    suspend fun update(position: FinancePosition)

    @Delete
    suspend fun delete(position: FinancePosition)

    @Query("DELETE FROM finance_positions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM finance_positions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositions(positions: List<FinancePosition>)

    @Query("SELECT * FROM finance_positions WHERE productName = :productName LIMIT 1")
    suspend fun getByName(productName: String): FinancePosition?

    @Query("SELECT * FROM finance_positions")
    suspend fun getAllPositionsList(): List<FinancePosition>
}