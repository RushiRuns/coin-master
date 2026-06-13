package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import java.util.Calendar
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity): Result<Long> {
        // 1. Validation
        if (transaction.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }

        val account = accountRepository.getAccountById(transaction.accountId)
            ?: return Result.failure(IllegalArgumentException("Source account does not exist."))

        if (transaction.type == TransactionType.EXPENSE) {
            if (transaction.categoryId == null) {
                return Result.failure(IllegalArgumentException("Category must be selected for expenses."))
            }
        }

        if (transaction.type == TransactionType.TRANSFER) {
            if (transaction.transferToAccountId == null) {
                return Result.failure(IllegalArgumentException("Destination account must be selected for transfers."))
            }
            if (transaction.accountId == transaction.transferToAccountId) {
                return Result.failure(IllegalArgumentException("Source and destination accounts must be different."))
            }
            val targetAccount = accountRepository.getAccountById(transaction.transferToAccountId)
                ?: return Result.failure(IllegalArgumentException("Destination account does not exist."))
        }

        // 2. Budget Period checks
        val budgetPeriod = budgetRepository.getOrCreateBudgetPeriodForDate(transaction.date)

        // 3. Save transaction with correct budget period ID set
        val finalTransaction = transaction.copy(budgetPeriodId = budgetPeriod.id)
        return try {
            val id = transactionRepository.insertTransaction(finalTransaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
