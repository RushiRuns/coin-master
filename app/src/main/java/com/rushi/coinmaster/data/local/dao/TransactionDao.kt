package com.rushi.coinmaster.data.local.dao

import androidx.room.*
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.TransactionType
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

    @Query("UPDATE accounts SET balance_paise = balance_paise + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Long)

    @Query("UPDATE accounts SET balance_paise = :balance WHERE id = :accountId")
    suspend fun setAccountBalance(accountId: Long, balance: Long)

    @Transaction
    suspend fun insertTransactionAndUpdateAccount(transaction: TransactionEntity): Long {
        val id = insertTransaction(transaction)
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                updateAccountBalance(transaction.accountId, -transaction.amountPaise)
            }
            TransactionType.INCOME -> {
                updateAccountBalance(transaction.accountId, transaction.amountPaise)
            }
            TransactionType.TRANSFER -> {
                updateAccountBalance(transaction.accountId, -transaction.amountPaise)
                transaction.transferToAccountId?.let { toId ->
                    updateAccountBalance(toId, transaction.amountPaise)
                }
            }
            TransactionType.BALANCE_CORRECTION -> {
                setAccountBalance(transaction.accountId, transaction.amountPaise)
            }
        }
        return id
    }

    @Transaction
    suspend fun deleteTransactionAndUpdateAccount(transactionId: Long) {
        val transaction = getTransactionById(transactionId) ?: return
        if (transaction.isDeleted) return
        
        softDeleteTransaction(transactionId)
        
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                updateAccountBalance(transaction.accountId, transaction.amountPaise)
            }
            TransactionType.INCOME -> {
                updateAccountBalance(transaction.accountId, -transaction.amountPaise)
            }
            TransactionType.TRANSFER -> {
                updateAccountBalance(transaction.accountId, transaction.amountPaise)
                transaction.transferToAccountId?.let { toId ->
                    updateAccountBalance(toId, -transaction.amountPaise)
                }
            }
            TransactionType.BALANCE_CORRECTION -> {
                // Cannot mathematically revert correction without previous balance snapshot.
            }
        }
    }
}
