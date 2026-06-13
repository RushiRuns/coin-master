package com.rushi.coinmaster.ui.home

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.TransactionRepository
import com.rushi.coinmaster.data.repository.DebtRepository
import com.rushi.coinmaster.domain.usecase.GetNetWorthUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val accountRepository: AccountRepository = mockk()
    private val budgetRepository: BudgetRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val debtRepository: DebtRepository = mockk()
    private val getNetWorthUseCase: GetNetWorthUseCase = mockk()

    private lateinit var viewModel: HomeViewModel

    private val testBudgetPeriod = BudgetPeriodEntity(
        id = 1,
        startDate = System.currentTimeMillis() - 100000L,
        endDate = System.currentTimeMillis() + 1000000L,
        incomePaise = 5000000L,
        needsPercent = 50,
        wantsPercent = 30,
        savingsPercent = 20,
        isActive = true
    )

    private val testEnvelopes = listOf(
        EnvelopeWithAllocation(
            categoryId = 10L,
            categoryName = "Groceries",
            bucketType = BucketType.NEEDS,
            colorHex = "#E57373",
            iconName = "ic_groceries",
            allocatedAmountPaise = 20000L,
            spentAmountPaise = 5000L
        ),
        EnvelopeWithAllocation(
            categoryId = 11L,
            categoryName = "Dining Out",
            bucketType = BucketType.WANTS,
            colorHex = "#FFD54F",
            iconName = "ic_dining",
            allocatedAmountPaise = 15000L,
            spentAmountPaise = 2000L
        )
    )

    private val testCategories = listOf(
        CategoryEntity(id = 10L, name = "Groceries", bucketType = BucketType.NEEDS, colorHex = "#E57373", iconName = "ic_groceries", displayOrder = 0),
        CategoryEntity(id = 11L, name = "Dining Out", bucketType = BucketType.WANTS, colorHex = "#FFD54F", iconName = "ic_dining", displayOrder = 1)
    )

    private val testTransactions = listOf(
        TransactionEntity(id = 100L, amountPaise = 5000L, type = TransactionType.EXPENSE, accountId = 2L, categoryId = 10L, date = 1686000000000L, note = "Weekly food"),
        TransactionEntity(id = 101L, amountPaise = 2000L, type = TransactionType.EXPENSE, accountId = 1L, categoryId = 11L, date = 1686010000000L, note = "Dinner")
    )

    private val testAccounts = listOf(
        AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, balancePaise = 10000L, colorHex = "#000", iconName = "ic_cash"),
        AccountEntity(id = 2L, name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 50000L, colorHex = "#FFF", iconName = "ic_bank")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock repository flows
        every { accountRepository.getAccountsFlow() } returns flowOf(testAccounts)
        
        every { budgetRepository.getBudgetPeriodsFlow() } returns flowOf(listOf(testBudgetPeriod))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(testBudgetPeriod.id) } returns flowOf(testEnvelopes)
        every { budgetRepository.getCategoriesFlow() } returns flowOf(testCategories)
        every { transactionRepository.getRecentTransactionsFlow(7) } returns flowOf(testTransactions)
        every { debtRepository.getDebtsFlow() } returns flowOf(emptyList())
        every { getNetWorthUseCase(testAccounts, any()) } returns 60000L
 
        viewModel = HomeViewModel(
            accountRepository = accountRepository,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            debtRepository = debtRepository,
            getNetWorthUseCase = getNetWorthUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiStateEmitsCorrectDashboardSummary() = runTest {
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {
                states.add(it)
            }
        }

        testScheduler.advanceUntilIdle()

        val lastState = states.last()
        assertEquals(60000L, lastState.netWorth)
        assertEquals(testAccounts, lastState.accounts)
        assertEquals(testBudgetPeriod.id, lastState.budgetPeriod?.id)
        assertEquals(testEnvelopes, lastState.envelopes)
        assertEquals(35000L, lastState.totalBudgetedPaise)
        assertEquals(7000L, lastState.totalSpentPaise)
        
        // Check recent transactions mapping
        assertEquals(2, lastState.recentTransactions.size)
        val firstTx = lastState.recentTransactions[0]
        assertEquals(100L, firstTx.id)
        assertEquals(5000L, firstTx.amountPaise)
        assertEquals(TransactionType.EXPENSE, firstTx.type)
        assertEquals("Bank", firstTx.accountName)
        assertEquals("Groceries", firstTx.categoryName)
        assertEquals("#E57373", firstTx.categoryColorHex)
        assertEquals("Weekly food", firstTx.note)
    }

    @Test
    fun testCategorySelectionUpdatesDetail() = runTest {
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {
                states.add(it)
            }
        }

        testScheduler.advanceUntilIdle()
        assertNull(states.last().selectedCategoryDetail)

        // Select Groceries
        viewModel.selectCategory(10L)
        testScheduler.advanceUntilIdle()

        assertEquals("Groceries", states.last().selectedCategoryDetail?.categoryName)
        assertEquals(5000L, states.last().selectedCategoryDetail?.spentAmountPaise)

        // Select None
        viewModel.selectCategory(null)
        testScheduler.advanceUntilIdle()
        assertNull(states.last().selectedCategoryDetail)
    }
}
