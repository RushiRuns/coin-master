package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
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

        // 2. Budget Month checks
        val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val budgetMonthId = year * 100 + month

        val budgetMonth = budgetRepository.getBudgetMonth(budgetMonthId)
        if (budgetMonth == null) {
            val defaultBudgetMonth = BudgetMonthEntity(
                id = budgetMonthId,
                month = month,
                year = year,
                incomePaise = 0L,
                needsPercent = 50,
                wantsPercent = 30,
                savingsPercent = 20,
                isActive = false
            )
            budgetRepository.insertBudgetMonth(defaultBudgetMonth)
        }

        // 3. Save transaction with correct budget month ID set
        val finalTransaction = transaction.copy(budgetMonthId = budgetMonthId)
        return try {
            val id = transactionRepository.insertTransaction(finalTransaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
