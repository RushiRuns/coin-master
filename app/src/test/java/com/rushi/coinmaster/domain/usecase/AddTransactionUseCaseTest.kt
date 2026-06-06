package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionUseCaseTest {

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)

    private val useCase = AddTransactionUseCase(transactionRepository, accountRepository, budgetRepository)

    @Test
    fun testAmountMustBePositive() = runBlocking {
        val transaction = TransactionEntity(
            amountPaise = 0L,
            type = TransactionType.INCOME,
            accountId = 1L,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Amount must be greater than zero.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testSourceAccountMustExist() = runBlocking {
        coEvery { accountRepository.getAccountById(1L) } returns null

        val transaction = TransactionEntity(
            amountPaise = 500L,
            type = TransactionType.INCOME,
            accountId = 1L,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Source account does not exist.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testExpenseRequiresCategory() = runBlocking {
        val account = AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, colorHex = "", iconName = "")
        coEvery { accountRepository.getAccountById(1L) } returns account

        val transaction = TransactionEntity(
            amountPaise = 500L,
            type = TransactionType.EXPENSE,
            accountId = 1L,
            categoryId = null,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Category must be selected for expenses.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testTransferRequiresDestinationAccount() = runBlocking {
        val account = AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, colorHex = "", iconName = "")
        coEvery { accountRepository.getAccountById(1L) } returns account

        val transaction = TransactionEntity(
            amountPaise = 500L,
            type = TransactionType.TRANSFER,
            accountId = 1L,
            transferToAccountId = null,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Destination account must be selected for transfers.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testTransferDestinationMustExist() = runBlocking {
        val account = AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, colorHex = "", iconName = "")
        coEvery { accountRepository.getAccountById(1L) } returns account
        coEvery { accountRepository.getAccountById(2L) } returns null

        val transaction = TransactionEntity(
            amountPaise = 500L,
            type = TransactionType.TRANSFER,
            accountId = 1L,
            transferToAccountId = 2L,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Destination account does not exist.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testTransferSourceAndDestinationMustBeDifferent() = runBlocking {
        val account = AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, colorHex = "", iconName = "")
        coEvery { accountRepository.getAccountById(1L) } returns account

        val transaction = TransactionEntity(
            amountPaise = 500L,
            type = TransactionType.TRANSFER,
            accountId = 1L,
            transferToAccountId = 1L,
            date = System.currentTimeMillis()
        )

        val result = useCase(transaction)

        assertTrue(result.isFailure)
        assertEquals("Source and destination accounts must be different.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testAutoCreatesBudgetMonthIfMissing() = runBlocking {
        val account = AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, colorHex = "", iconName = "")
        coEvery { accountRepository.getAccountById(1L) } returns account
        coEvery { budgetRepository.getBudgetMonth(202606) } returns null

        val transaction = TransactionEntity(
            amountPaise = 50000L,
            type = TransactionType.INCOME,
            accountId = 1L,
            date = 1780704000000L // 6th June 2026 UTC/local approx
        )

        coEvery { transactionRepository.insertTransaction(any()) } returns 10L

        val result = useCase(transaction)

        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())

        coVerify {
            budgetRepository.insertBudgetMonth(match {
                it.id == 202606 && it.month == 6 && it.year == 2026 && it.incomePaise == 0L
            })
        }
    }
}
