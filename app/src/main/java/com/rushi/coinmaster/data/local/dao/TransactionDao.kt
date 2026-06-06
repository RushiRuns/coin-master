package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date DESC")
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date DESC")
    suspend fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND is_deleted = 0 ORDER BY date DESC")
    fun getTransactionsForAccountFlow(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long)
}
