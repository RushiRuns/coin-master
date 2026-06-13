package com.rushi.coinmaster.data.repository

import androidx.room.withTransaction
import com.rushi.coinmaster.data.local.dao.DebtDao
import com.rushi.coinmaster.data.local.dao.TransactionDao
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.DebtType
import com.rushi.coinmaster.data.local.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val db: CoinMasterDatabase,
    private val debtDao: DebtDao,
    private val transactionDao: TransactionDao
) {
    fun getDebtsFlow(): Flow<List<DebtEntity>> = debtDao.getDebtsFlow()

    fun getActiveDebtsFlow(): Flow<List<DebtEntity>> = debtDao.getActiveDebtsFlow()

    fun getSettledDebtsFlow(): Flow<List<DebtEntity>> = debtDao.getSettledDebtsFlow()

    suspend fun getDebtById(id: Long): DebtEntity? = debtDao.getDebtById(id)

    fun getDebtByIdFlow(id: Long): Flow<DebtEntity?> = debtDao.getDebtByIdFlow(id)

    fun getTransactionsForDebtFlow(debtId: Long): Flow<List<TransactionEntity>> =
        debtDao.getTransactionsForDebtFlow(debtId)

    suspend fun insertDebtWithTransaction(
        debt: DebtEntity,
        accountId: Long,
        categoryId: Long?,
        dateMillis: Long
    ): Long = db.withTransaction {
        val debtId = debtDao.insertDebt(debt)
        
        // Generate initial transaction that updates account balance
        val transactionType = if (debt.type == DebtType.LENT) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }
        
        val note = debt.note ?: if (debt.type == DebtType.LENT) {
            "Lent to ${debt.personName}"
        } else {
            "Borrowed from ${debt.personName}"
        }

        // Determine budget month ID (format: YYYYMM)
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
        val budgetMonthId = calendar.get(java.util.Calendar.YEAR) * 100 + (calendar.get(java.util.Calendar.MONTH) + 1)

        val initialTransaction = TransactionEntity(
            amountPaise = debt.amountPaise,
            type = transactionType,
            accountId = accountId,
            categoryId = categoryId,
            budgetMonthId = budgetMonthId,
            debtId = debtId,
            date = dateMillis,
            note = note
        )
        
        transactionDao.insertTransactionAndUpdateAccount(initialTransaction)
        
        // Return debtId but make sure the entity has its generated id set
        debtId
    }

    suspend fun recordRepayment(
        debtId: Long,
        amountPaise: Long,
        accountId: Long,
        dateMillis: Long,
        note: String?
    ) = db.withTransaction {
        val debt = debtDao.getDebtById(debtId) ?: throw IllegalArgumentException("Debt not found")
        
        val newRemaining = maxOf(0L, debt.remainingPaise - amountPaise)
        val isSettled = newRemaining == 0L
        
        debtDao.updateDebt(debt.copy(
            remainingPaise = newRemaining,
            isSettled = isSettled
        ))

        // Repaying borrowed money: cash goes OUT (EXPENSE)
        // Receiving repayment on lent money: cash comes IN (INCOME)
        val transactionType = if (debt.type == DebtType.LENT) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val formattedNote = note ?: if (debt.type == DebtType.LENT) {
            "Repayment from ${debt.personName}"
        } else {
            "Repayment to ${debt.personName}"
        }

        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
        val budgetMonthId = calendar.get(java.util.Calendar.YEAR) * 100 + (calendar.get(java.util.Calendar.MONTH) + 1)

        val repaymentTransaction = TransactionEntity(
            amountPaise = amountPaise,
            type = transactionType,
            accountId = accountId,
            budgetMonthId = budgetMonthId,
            debtId = debtId,
            date = dateMillis,
            note = formattedNote
        )

        transactionDao.insertTransactionAndUpdateAccount(repaymentTransaction)
    }

    suspend fun deleteDebt(debtId: Long) = db.withTransaction {
        // SQLite foreign key with ON DELETE SET_NULL will set debt_id to NULL
        // on associated transactions automatically.
        debtDao.deleteDebtById(debtId)
    }
}
