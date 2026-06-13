package com.rushi.coinmaster.ui.budget

import com.rushi.coinmaster.data.local.entity.BudgetPeriodEntity
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.EnvelopeWithAllocation
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.domain.usecase.ValidateZeroBalanceUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: android.content.Context = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val validateZeroBalanceUseCase = ValidateZeroBalanceUseCase()

    private lateinit var viewModel: BudgetViewModel

    private val period1 = BudgetPeriodEntity(id = 1, startDate = 1000L, endDate = 2000L, incomePaise = 1_000_000L, needsPercent = 50, wantsPercent = 30, savingsPercent = 20)
    private val period2 = BudgetPeriodEntity(id = 2, startDate = 3000L, endDate = 4000L, incomePaise = 1_000_000L, needsPercent = 50, wantsPercent = 30, savingsPercent = 20)
    private val period3 = BudgetPeriodEntity(id = 3, startDate = 5000L, endDate = 6000L, incomePaise = 1_000_000L, needsPercent = 50, wantsPercent = 30, savingsPercent = 20)

    private val testPeriods = listOf(period1, period2, period3)

    private val balancedEnvelopes = listOf(
        EnvelopeWithAllocation(
            categoryId = 1L,
            categoryName = "Groceries",
            bucketType = BucketType.NEEDS,
            colorHex = "#FF5733",
            iconName = "ic_grocery",
            allocatedAmountPaise = 1_000_000L,
            spentAmountPaise = 0L
        )
    )

    private val unbalancedEnvelopes = listOf(
        EnvelopeWithAllocation(
            categoryId = 1L,
            categoryName = "Groceries",
            bucketType = BucketType.NEEDS,
            colorHex = "#FF5733",
            iconName = "ic_grocery",
            allocatedAmountPaise = 900_000L,
            spentAmountPaise = 0L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { budgetRepository.getBudgetPeriods() } returns testPeriods
        every { budgetRepository.getBudgetPeriodsFlow() } returns MutableStateFlow(testPeriods)
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(emptyList())
        every { budgetRepository.getCategoriesFlow() } returns MutableStateFlow(emptyList())
        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Period Navigation ───────────────────────────────────────────────────

    @Test
    fun `selectPreviousPeriod moves to chronologically previous period in list`() = runTest {
        viewModel.selectPeriod(2)
        viewModel.selectPreviousPeriod()
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.selectedPeriodId.value)
    }

    @Test
    fun `selectNextPeriod moves to chronologically next period in list`() = runTest {
        viewModel.selectPeriod(2)
        viewModel.selectNextPeriod()
        testScheduler.advanceUntilIdle()
        assertEquals(3, viewModel.selectedPeriodId.value)
    }

    @Test
    fun `selectPreviousPeriod does nothing when on the first period`() = runTest {
        viewModel.selectPeriod(1)
        viewModel.selectPreviousPeriod()
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.selectedPeriodId.value)
    }

    @Test
    fun `selectNextPeriod does nothing when on the last period`() = runTest {
        viewModel.selectPeriod(3)
        viewModel.selectNextPeriod()
        testScheduler.advanceUntilIdle()
        assertEquals(3, viewModel.selectedPeriodId.value)
    }

    // ── Budget Activation — Repository Interaction ──────────────────────────

    @Test
    fun `activateBudgetPeriod does NOT update repository when budget is unbalanced`() = runTest {
        every { budgetRepository.getBudgetPeriodsFlow() } returns MutableStateFlow(listOf(period1))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(unbalancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectPeriod(1)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.budgetPeriodState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unallocatedState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.activateBudgetPeriod()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { budgetRepository.updateBudgetPeriod(any()) }
    }

    @Test
    fun `activateBudgetPeriod updates repository with isActive=true when budget is balanced`() = runTest {
        every { budgetRepository.getBudgetPeriodsFlow() } returns MutableStateFlow(listOf(period1))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(balancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectPeriod(1)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.budgetPeriodState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unallocatedState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.activateBudgetPeriod()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { budgetRepository.updateBudgetPeriod(match { it.isActive && it.id == 1 }) }
    }

    // ── Validation Logic ────────────────────────────────────────────────────

    @Test
    fun `unallocatedState is valid only when allocations exactly equal income`() = runTest {
        every { budgetRepository.getBudgetPeriodsFlow() } returns MutableStateFlow(listOf(period1))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(balancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectPeriod(1)

        var validation = viewModel.unallocatedState.first()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.budgetPeriodState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.unallocatedState.collect { validation = it }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("Balanced budget should pass validation", validation.isValid)
        assertEquals(0L, validation.differencePaise)
    }

    // ── Save Category Validation ─────────────────────────────────────────────

    @Test
    fun `saveCategory calls insertCategory when id is zero and name is valid`() = runTest {
        viewModel.saveCategory(
            id = 0L,
            name = "Rent",
            bucketType = BucketType.NEEDS,
            colorHex = "#00BCD4",
            iconName = "ic_home"
        )
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetRepository.insertCategory(match { it.name == "Rent" && it.bucketType == BucketType.NEEDS })
        }
    }

    @Test
    fun `saveCategory with initial allocation calls saveAllocation when category is new`() = runTest {
        coEvery { budgetRepository.insertCategory(any()) } returns 123L
        viewModel.selectPeriod(1)

        viewModel.saveCategory(
            id = 0L,
            name = "Rent",
            bucketType = BucketType.NEEDS,
            colorHex = "#00BCD4",
            iconName = "ic_home",
            initialAllocationPaise = 500_000L
        )
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetRepository.insertCategory(match { it.name == "Rent" })
            budgetRepository.saveAllocation(1, 123L, 500_000L)
        }
    }

    @Test
    fun `copyAllocationsFromPreviousPeriod invokes copyAllocations with correct previous period ID`() = runTest {
        viewModel.selectPeriod(2)
        viewModel.copyAllocationsFromPreviousPeriod()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetRepository.copyAllocations(1, 2)
        }
    }

    @Test
    fun `saveCategory rejects duplicate name case-insensitively`() = runTest {
        val existingCategories = listOf(
            CategoryEntity(id = 10L, name = "Groceries", bucketType = BucketType.NEEDS, colorHex = "#FF5733", iconName = "ic_grocery", displayOrder = 0)
        )
        every { budgetRepository.getCategoriesFlow() } returns MutableStateFlow(existingCategories)
        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)

        val events = mutableListOf<BudgetUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveCategory(
            id = 0L,
            name = "groceries",
            bucketType = BucketType.NEEDS,
            colorHex = "#00BCD4",
            iconName = "ic_home"
        )
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { budgetRepository.insertCategory(any()) }
        assertTrue(events.any { it is BudgetUiEvent.Error && it.message == "An envelope with this name already exists." })
    }

    @Test
    fun `assignCategoryToBucket updates category with target bucketType`() = runTest {
        val targetCategory = CategoryEntity(id = 12L, name = "Rent", bucketType = null, colorHex = "#00BCD4", iconName = "ic_home", displayOrder = 0)
        every { budgetRepository.getCategoriesFlow() } returns MutableStateFlow(listOf(targetCategory))
        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)

        viewModel.assignCategoryToBucket(12L, BucketType.NEEDS)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetRepository.updateCategory(match { it.id == 12L && it.bucketType == BucketType.NEEDS })
        }
    }
}
