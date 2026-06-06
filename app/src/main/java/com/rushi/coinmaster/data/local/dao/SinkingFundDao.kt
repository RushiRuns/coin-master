package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.SinkingFundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SinkingFundDao {

    @Query("SELECT * FROM sinking_funds")
    fun getSinkingFundsFlow(): Flow<List<SinkingFundEntity>>

    @Query("SELECT * FROM sinking_funds")
    suspend fun getSinkingFunds(): List<SinkingFundEntity>

    @Query("SELECT * FROM sinking_funds WHERE id = :id")
    suspend fun getSinkingFundById(id: Long): SinkingFundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSinkingFund(sinkingFund: SinkingFundEntity): Long

    @Update
    suspend fun updateSinkingFund(sinkingFund: SinkingFundEntity)

    @Delete
    suspend fun deleteSinkingFund(sinkingFund: SinkingFundEntity)
}
