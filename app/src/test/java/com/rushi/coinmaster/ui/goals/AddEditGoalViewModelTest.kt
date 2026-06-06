package com.rushi.coinmaster.ui.goals

import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.SinkingFundRepository
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditGoalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: android.content.Context = mockk(relaxed = true)
    private val sinkingFundRepository: SinkingFundRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)

    private lateinit var viewModel: AddEditGoalViewModel

    private val savingsCats = listOf(
        CategoryEntity(id = 5L, name = "Emergency Fund", bucketType = BucketType.SAVINGS, colorHex = "#FF8A65", iconName = "ic_emergency", displayOrder = 0)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { budgetRepository.getCategoriesFlow() } returns flowOf(savingsCats)
        viewModel = AddEditGoalViewModel(context, sinkingFundRepository, budgetRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSavingsCategoriresFilteredToSavingsBucket() = runTest {
        val results = mutableListOf<List<CategoryEntity>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.savingsCategories.collect { results.add(it) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(1, results.last().size)
        assertEquals(BucketType.SAVINGS, results.last().first().bucketType)
    }

    @Test
    fun testSaveGoalEmitsErrorOnEmptyName() = runTest {
        val events = mutableListOf<AddEditGoalEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.saveGoal(
            id = 0L,
            name = "   ",
            targetAmountPaise = 10000_00L,
            targetDate = System.currentTimeMillis() + 86400000L,
            autoCreateCategory = true,
            selectedCategoryId = null
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assert(events.first() is AddEditGoalEvent.Error)
    }

    @Test
    fun testSaveGoalEmitsErrorOnZeroAmount() = runTest {
        val events = mutableListOf<AddEditGoalEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.saveGoal(
            id = 0L,
            name = "Laptop",
            targetAmountPaise = 0L,
            targetDate = System.currentTimeMillis() + 86400000L,
            autoCreateCategory = true,
            selectedCategoryId = null
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assert(events.first() is AddEditGoalEvent.Error)
    }

    @Test
    fun testSaveGoalWithAutoCreateCallsInsertAndEmitsSuccess() = runTest {
        val events = mutableListOf<AddEditGoalEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        val targetDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, 6)
        }.timeInMillis

        coEvery { budgetRepository.insertCategory(any()) } returns 99L
        coEvery { sinkingFundRepository.insertSinkingFund(any()) } returns 1L

        viewModel.saveGoal(
            id = 0L,
            name = "MacBook Pro",
            targetAmountPaise = 150000_00L,
            targetDate = targetDate,
            autoCreateCategory = true,
            selectedCategoryId = null
        )
        testScheduler.advanceUntilIdle()

        coVerify { budgetRepository.insertCategory(match { it.name == "MacBook Pro" && it.bucketType == BucketType.SAVINGS }) }
        coVerify { sinkingFundRepository.insertSinkingFund(match { it.name == "MacBook Pro" && it.categoryId == 99L }) }
        assertEquals(1, events.size)
        assert(events.first() is AddEditGoalEvent.Success)
    }

    @Test
    fun testSaveGoalWithExistingCategoryReusesCategoryId() = runTest {
        val events = mutableListOf<AddEditGoalEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        // Pre-load categories so viewModel knows about "Emergency Fund"
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.savingsCategories.collect {}
        }
        testScheduler.advanceUntilIdle()

        val targetDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, 3)
        }.timeInMillis

        coEvery { sinkingFundRepository.insertSinkingFund(any()) } returns 2L

        // Reuse the "Emergency Fund" category by using same name with auto-create
        viewModel.saveGoal(
            id = 0L,
            name = "Emergency Fund",
            targetAmountPaise = 50000_00L,
            targetDate = targetDate,
            autoCreateCategory = true,
            selectedCategoryId = null
        )
        testScheduler.advanceUntilIdle()

        // Should NOT insert a new category since one with the same name exists
        coVerify(exactly = 0) { budgetRepository.insertCategory(any()) }
        // Should insert the fund with existing category id 5L
        coVerify { sinkingFundRepository.insertSinkingFund(match { it.categoryId == 5L }) }
        assertEquals(1, events.size)
        assert(events.first() is AddEditGoalEvent.Success)
    }
}
