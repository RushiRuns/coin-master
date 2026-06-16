package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.IncomeStreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeStreamDao {
    @Query("SELECT * FROM income_streams WHERE is_deleted = 0 ORDER BY name ASC")
    fun getIncomeStreamsFlow(): Flow<List<IncomeStreamEntity>>

    @Query("SELECT * FROM income_streams WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getIncomeStreams(): List<IncomeStreamEntity>

    @Query("SELECT * FROM income_streams WHERE id = :id AND is_deleted = 0")
    suspend fun getIncomeStreamById(id: Long): IncomeStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeStream(incomeStream: IncomeStreamEntity): Long

    @Update
    suspend fun updateIncomeStream(incomeStream: IncomeStreamEntity)

    @Query("UPDATE income_streams SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteIncomeStream(id: Long)
}
