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

    suspend fun getTransactions(): List<TransactionEntity> = transactionDao.getTransactions()

    fun getTransactionsForAccountFlow(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForAccountFlow(accountId)

    suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransactionAndUpdateAccount(transaction)

    suspend fun deleteTransaction(transactionId: Long) =
        transactionDao.deleteTransactionAndUpdateAccount(transactionId)
}
