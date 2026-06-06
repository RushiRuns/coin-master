package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.SinkingFundEntity
import com.rushi.coinmaster.data.local.model.SinkingFundWithProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface SinkingFundDao {

    @Query("SELECT * FROM sinking_funds")
    fun getSinkingFundsFlow(): Flow<List<SinkingFundEntity>>

    @Transaction
    @Query("""
        SELECT sf.*, c.name AS category_name,
               (COALESCE((SELECT SUM(ea.allocated_amount_paise) FROM envelope_allocations ea WHERE ea.category_id = sf.category_id), 0) - 
                COALESCE((SELECT SUM(t.amount_paise) FROM transactions t WHERE t.category_id = sf.category_id AND t.type = 'EXPENSE' AND t.is_deleted = 0), 0) +
                COALESCE((SELECT SUM(t.amount_paise) FROM transactions t WHERE t.category_id = sf.category_id AND t.type = 'INCOME' AND t.is_deleted = 0), 0)
               ) AS computed_saved_amount
        FROM sinking_funds sf
        INNER JOIN categories c ON sf.category_id = c.id
    """)
    fun getSinkingFundsWithProgressFlow(): Flow<List<SinkingFundWithProgress>>

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

