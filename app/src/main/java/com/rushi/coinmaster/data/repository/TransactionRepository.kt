package com.rushi.coinmaster.data.repository

import com.rushi.coinmaster.data.local.dao.TransactionDao
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getTransactionsFlow(): Flow<List<TransactionEntity>> = transactionDao.getTransactionsFlow()

    /**
     * Returns a Flow of the most recent [limit] transactions ordered by date descending.
     * Uses SQL-level LIMIT for efficiency — avoids loading the entire transaction history
     * into memory when only a summary view is needed (e.g., Dashboard).
     */
    fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactionsFlow(limit)

    suspend fun getTransactions(): List<TransactionEntity> = transactionDao.getTransactions()

    fun getTransactionsForAccountFlow(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForAccountFlow(accountId)

    suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransactionAndUpdateAccount(transaction)

    suspend fun deleteTransaction(transactionId: Long) =
        transactionDao.deleteTransactionAndUpdateAccount(transactionId)
}
