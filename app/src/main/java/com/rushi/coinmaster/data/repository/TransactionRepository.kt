package com.rushi.coinmaster.data.repository

import android.content.Context
import com.rushi.coinmaster.data.local.dao.TransactionDao
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransactionAndUpdateAccount(transaction)
        WidgetUpdater.updateWidget(context)
        return id
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransactionAndUpdateAccount(transactionId)
        WidgetUpdater.updateWidget(context)
    }
}
