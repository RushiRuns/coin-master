package com.rushi.coinmaster.ui.transactions

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val addTransactionUseCase: AddTransactionUseCase = mockk()

    private lateinit var viewModel: TransactionViewModel

    private val testAccounts = listOf(
        AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 10000L, colorHex = "", iconName = "")
    )
    private val testCategories = listOf(
        CategoryEntity(id = 1L, name = "Food", bucketType = BucketType.NEEDS, colorHex = "", iconName = "", displayOrder = 0)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { accountRepository.getAccountsFlow() } returns flowOf(testAccounts)
        every { budgetRepository.getCategoriesFlow() } returns flowOf(testCategories)

        viewModel = TransactionViewModel(accountRepository, budgetRepository, addTransactionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExposesAccountsAndCategories() = runTest {
        val accounts = mutableListOf<List<AccountEntity>>()
        val categories = mutableListOf<List<CategoryEntity>>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.accountsState.collect { accounts.add(it) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categoriesState.collect { categories.add(it) }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(testAccounts, viewModel.accountsState.value)
        assertEquals(testCategories, viewModel.categoriesState.value)
    }

    @Test
    fun testSaveTransactionValidationErrors() = runTest {
        val events = mutableListOf<TransactionUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        // Blank amount
        viewModel.saveTransaction("", TransactionType.INCOME, 1L, null, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertTrue(events.last() is TransactionUiEvent.Error)
        assertEquals("Amount cannot be empty.", (events.last() as TransactionUiEvent.Error).message)

        // Invalid amount
        viewModel.saveTransaction("-10.00", TransactionType.INCOME, 1L, null, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertEquals("Amount must be greater than zero.", (events.last() as TransactionUiEvent.Error).message)

        // Source account not selected
        viewModel.saveTransaction("100.00", TransactionType.INCOME, 0L, null, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertEquals("Please select a source account.", (events.last() as TransactionUiEvent.Error).message)

        // Expense without category
        viewModel.saveTransaction("100.00", TransactionType.EXPENSE, 1L, null, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertEquals("Please select a category.", (events.last() as TransactionUiEvent.Error).message)

        // Transfer without destination account
        viewModel.saveTransaction("100.00", TransactionType.TRANSFER, 1L, null, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertEquals("Please select a destination account.", (events.last() as TransactionUiEvent.Error).message)

        // Transfer with same accounts
        viewModel.saveTransaction("100.00", TransactionType.TRANSFER, 1L, 1L, null, System.currentTimeMillis(), "")
        testScheduler.advanceUntilIdle()
        assertEquals("Source and destination accounts must be different.", (events.last() as TransactionUiEvent.Error).message)
    }

    @Test
    fun testSaveTransactionSuccess() = runTest {
        val events = mutableListOf<TransactionUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        coEvery { addTransactionUseCase(any()) } returns Result.success(1L)

        viewModel.saveTransaction("150.00", TransactionType.INCOME, 1L, null, null, System.currentTimeMillis(), "Salary")
        testScheduler.advanceUntilIdle()

        assertTrue(events.last() is TransactionUiEvent.Success)
        coVerify { addTransactionUseCase(match {
            it.amountPaise == 15000L && it.type == TransactionType.INCOME && it.accountId == 1L && it.note == "Salary"
        }) }
    }
}
