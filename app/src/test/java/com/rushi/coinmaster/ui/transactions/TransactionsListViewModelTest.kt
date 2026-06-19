package com.rushi.coinmaster.ui.transactions

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)

    private lateinit var viewModel: TransactionsListViewModel

    private val testTransactions = listOf(
        TransactionEntity(
            id = 1L,
            amountPaise = 10000L,
            type = TransactionType.EXPENSE,
            accountId = 1L,
            categoryId = 1L,
            date = System.currentTimeMillis(),
            note = "Burger"
        )
    )
    private val testAccounts = listOf(
        AccountEntity(id = 1L, name = "Cash", type = AccountType.WALLET, balancePaise = 10000L, colorHex = "", iconName = "")
    )
    private val testCategories = listOf(
        CategoryEntity(id = 1L, name = "Food", bucketType = BucketType.NEEDS, colorHex = "", iconName = "", displayOrder = 0)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { accountRepository.getAccountsFlow() } returns flowOf(testAccounts)
        every { budgetRepository.getCategoriesFlow() } returns flowOf(testCategories)
        every { transactionRepository.getTransactionsBetweenDatesFlow(any(), any()) } returns flowOf(testTransactions)

        viewModel = TransactionsListViewModel(transactionRepository, accountRepository, budgetRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateExposesTransactions() = runTest {
        val states = mutableListOf<TransactionsUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.transactions.size)
        val item = viewModel.uiState.value.transactions.first()
        assertEquals(1L, item.id)
        assertEquals(10000L, item.amountPaise)
        assertEquals("Cash", item.accountName)
        assertEquals("Food", item.categoryName)
        assertEquals("Burger", item.note)
    }

    @Test
    fun testSetFilterTriggersQueryUpdate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setFilter(TransactionFilter.MONTH)
        testScheduler.advanceUntilIdle()

        assertEquals(TransactionFilter.MONTH, viewModel.selectedFilter.value)
        verify { transactionRepository.getTransactionsBetweenDatesFlow(any(), any()) }
    }

    @Test
    fun testSetTabAndDateTriggersQueryUpdate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setTab(1)
        viewModel.setDate(2000L)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.activeTab.value)
        assertEquals(2000L, viewModel.selectedDateMillis.value)
        verify { transactionRepository.getTransactionsBetweenDatesFlow(any(), any()) }
    }

    @Test
    fun testDeleteTransactionDelegatesToRepository() = runTest {
        viewModel.deleteTransaction(123L)
        testScheduler.advanceUntilIdle()

        coVerify { transactionRepository.deleteTransaction(123L) }
    }

    @Test
    fun testSearchQueryFiltersTransactions() = runTest {
        val extraTransactions = listOf(
            TransactionEntity(id = 1L, amountPaise = 10000L, type = TransactionType.EXPENSE, accountId = 1L, categoryId = 1L, date = System.currentTimeMillis(), note = "Burger"),
            TransactionEntity(id = 2L, amountPaise = 15000L, type = TransactionType.EXPENSE, accountId = 1L, categoryId = 1L, date = System.currentTimeMillis(), note = "Pizza")
        )
        every { transactionRepository.getTransactionsBetweenDatesFlow(any(), any()) } returns flowOf(extraTransactions)

        val freshViewModel = TransactionsListViewModel(transactionRepository, accountRepository, budgetRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // 1. Initial State: both show
        assertEquals(2, freshViewModel.uiState.value.transactions.size)

        // 2. Search for "Burger": only Burger matches
        freshViewModel.setSearchQuery("Burger")
        testScheduler.advanceUntilIdle()
        assertEquals(1, freshViewModel.uiState.value.transactions.size)
        assertEquals("Burger", freshViewModel.uiState.value.transactions.first().note)

        // 3. Search case-insensitively: "pizza"
        freshViewModel.setSearchQuery("pizza")
        testScheduler.advanceUntilIdle()
        assertEquals(1, freshViewModel.uiState.value.transactions.size)
        assertEquals("Pizza", freshViewModel.uiState.value.transactions.first().note)

        // 4. Search for category name "Food" (matches categoryName "Food")
        freshViewModel.setSearchQuery("Food")
        testScheduler.advanceUntilIdle()
        assertEquals(2, freshViewModel.uiState.value.transactions.size)

        // 5. Search for account "Cash"
        freshViewModel.setSearchQuery("Cash")
        testScheduler.advanceUntilIdle()
        assertEquals(2, freshViewModel.uiState.value.transactions.size)

        // 6. Search for non-matching query
        freshViewModel.setSearchQuery("Salad")
        testScheduler.advanceUntilIdle()
        assertEquals(0, freshViewModel.uiState.value.transactions.size)
    }
}
