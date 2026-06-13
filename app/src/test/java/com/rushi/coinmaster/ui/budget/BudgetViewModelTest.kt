package com.rushi.coinmaster.ui.budget

import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
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

/**
 * Unit tests for [BudgetViewModel].
 *
 * Covers:
 * - Month navigation (prev/next) arithmetic (Principle IX — reactive state)
 * - Zero-balance validation before activation (Principle IV — ZBB)
 * - Repository update called only when budget is balanced
 * - Blank envelope name rejection (saveCategory guard)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: android.content.Context = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val validateZeroBalanceUseCase = ValidateZeroBalanceUseCase()

    private lateinit var viewModel: BudgetViewModel

    private val balancedMonth = BudgetMonthEntity(
        id = 202601,
        month = 1,
        year = 2026,
        incomePaise = 1_000_000L,
        needsPercent = 50,
        wantsPercent = 30,
        savingsPercent = 20
    )

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
        every { budgetRepository.getBudgetMonthsFlow() } returns MutableStateFlow(emptyList())
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(emptyList())
        every { budgetRepository.getCategoriesFlow() } returns MutableStateFlow(emptyList())
        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Month Navigation ────────────────────────────────────────────────────

    @Test
    fun `selectPreviousMonth on January wraps to December of previous year`() {
        viewModel.selectMonth(202601)
        viewModel.selectPreviousMonth()
        assertEquals(202512, viewModel.selectedMonthId.value)
    }

    @Test
    fun `selectNextMonth on December wraps to January of next year`() {
        viewModel.selectMonth(202512)
        viewModel.selectNextMonth()
        assertEquals(202601, viewModel.selectedMonthId.value)
    }

    @Test
    fun `selectPreviousMonth decrements month within the same year`() {
        viewModel.selectMonth(202606)
        viewModel.selectPreviousMonth()
        assertEquals(202605, viewModel.selectedMonthId.value)
    }

    @Test
    fun `selectNextMonth increments month within the same year`() {
        viewModel.selectMonth(202604)
        viewModel.selectNextMonth()
        assertEquals(202605, viewModel.selectedMonthId.value)
    }

    // ── Budget Activation — Repository Interaction (Principle IV) ────────────
    // We test the repository interaction, not the UI event, since MutableSharedFlow
    // collection in unit tests depends on coroutine scheduling that varies by dispatcher.
    // The core invariant is: updateBudgetMonth must NOT be called when budget is unbalanced,
    // and MUST be called with isActive=true when balanced.

    @Test
    fun `activateBudgetMonth does NOT update repository when budget is unbalanced`() = runTest {
        every { budgetRepository.getBudgetMonthsFlow() } returns MutableStateFlow(listOf(balancedMonth))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(unbalancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectMonth(202601)

        // Activate state flows to populate budgetMonthState and unallocatedState
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.budgetMonthState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unallocatedState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.activateBudgetMonth()
        testScheduler.advanceUntilIdle()

        // Budget is unbalanced — repository must NOT be told to activate
        coVerify(exactly = 0) { budgetRepository.updateBudgetMonth(any()) }
    }

    @Test
    fun `activateBudgetMonth updates repository with isActive=true when budget is balanced`() = runTest {
        every { budgetRepository.getBudgetMonthsFlow() } returns MutableStateFlow(listOf(balancedMonth))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(balancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectMonth(202601)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.budgetMonthState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unallocatedState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.activateBudgetMonth()
        testScheduler.advanceUntilIdle()

        // Budget is balanced — repository must be updated with isActive = true
        coVerify(exactly = 1) { budgetRepository.updateBudgetMonth(match { it.isActive && it.id == 202601 }) }
    }

    @Test
    fun `activateBudgetMonth does nothing when budgetMonthState is null`() = runTest {
        // Default setUp has empty flow, so budgetMonthState.value == null
        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.budgetMonthState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.activateBudgetMonth()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { budgetRepository.updateBudgetMonth(any()) }
    }

    // ── Validation Logic (using real ValidateZeroBalanceUseCase) ─────────────

    @Test
    fun `unallocatedState is valid only when allocations exactly equal income`() = runTest {
        every { budgetRepository.getBudgetMonthsFlow() } returns MutableStateFlow(listOf(balancedMonth))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(balancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectMonth(202601)

        // Collect unallocatedState
        var validation = viewModel.unallocatedState.first()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.budgetMonthState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.unallocatedState.collect { validation = it }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("Balanced budget should pass validation", validation.isValid)
        assertEquals(0L, validation.differencePaise)
    }

    @Test
    fun `unallocatedState is invalid and shows positive difference when under-allocated`() = runTest {
        every { budgetRepository.getBudgetMonthsFlow() } returns MutableStateFlow(listOf(balancedMonth))
        every { budgetRepository.getEnvelopesWithAllocationsFlow(any()) } returns MutableStateFlow(unbalancedEnvelopes)

        viewModel = BudgetViewModel(context, budgetRepository, validateZeroBalanceUseCase)
        viewModel.selectMonth(202601)

        var validation = viewModel.unallocatedState.first()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.budgetMonthState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.unallocatedState.collect { validation = it }
        }
        testScheduler.advanceUntilIdle()

        assertFalse("Unbalanced budget should fail validation", validation.isValid)
        assertEquals(100_000L, validation.differencePaise) // ₹1,000 unallocated
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
        viewModel.selectMonth(202601)

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
            budgetRepository.saveAllocation(202601, 123L, 500_000L)
        }
    }

    @Test
    fun `copyAllocationsFromPreviousMonth invokes copyAllocations with correct previous month ID`() = runTest {
        viewModel.selectMonth(202601)
        viewModel.copyAllocationsFromPreviousMonth()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetRepository.copyAllocations(202512, 202601)
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
            name = "groceries", // lowercase duplicate
            bucketType = BucketType.NEEDS,
            colorHex = "#00BCD4",
            iconName = "ic_home"
        )
        testScheduler.advanceUntilIdle()

        // Verify duplicate error is emitted and database insert is NOT called
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


