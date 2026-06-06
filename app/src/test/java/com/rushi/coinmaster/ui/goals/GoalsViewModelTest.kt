package com.rushi.coinmaster.ui.goals

import com.rushi.coinmaster.data.local.entity.SinkingFundEntity
import com.rushi.coinmaster.data.local.model.SinkingFundWithProgress
import com.rushi.coinmaster.data.repository.SinkingFundRepository
import com.rushi.coinmaster.domain.usecase.ComputeSinkingFundMonthlyContributionUseCase
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
class GoalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sinkingFundRepository: SinkingFundRepository = mockk(relaxed = true)
    private val computeContributionUseCase: ComputeSinkingFundMonthlyContributionUseCase = mockk()

    private lateinit var viewModel: GoalsViewModel

    private val testEntities = listOf(
        SinkingFundWithProgress(
            sinkingFund = SinkingFundEntity(
                id = 1L,
                name = "New Laptop",
                targetAmountPaise = 60000_00L,
                savedAmountPaise = 0L, // Handled by SQL subqueries as computed_saved_amount
                targetDate = 1799999999000L,
                categoryId = 10L,
                isCompleted = false
            ),
            categoryName = "Laptop Savings",
            computedSavedAmountPaise = 20000_00L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { sinkingFundRepository.getSinkingFundsWithProgressFlow() } returns flowOf(testEntities)
        every {
            computeContributionUseCase(
                targetAmountPaise = 60000_00L,
                savedAmountPaise = 20000_00L,
                targetDate = 1799999999000L,
                currentDate = any()
            )
        } returns 5000_00L

        viewModel = GoalsViewModel(sinkingFundRepository, computeContributionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGoalsListEmitsMappedUiModels() = runTest {
        val goals = mutableListOf<List<GoalUiModel>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.goalsList.collect {
                goals.add(it)
            }
        }

        testScheduler.advanceUntilIdle()

        assertEquals(1, goals.last().size)
        val uiModel = goals.last().first()
        assertEquals("New Laptop", uiModel.name)
        assertEquals(20000_00L, uiModel.savedAmountPaise)
        assertEquals(60000_00L, uiModel.targetAmountPaise)
        assertEquals("Laptop Savings", uiModel.categoryName)
        assertEquals(5000_00L, uiModel.monthlySavingsNeededPaise)
        assertEquals(33, uiModel.percent)
        assertTrue(!uiModel.isCompleted)
    }

    @Test
    fun testDeleteGoalTriggersRepositoryDelete() = runTest {
        val entity = testEntities.first().sinkingFund
        coEvery { sinkingFundRepository.getSinkingFundById(1L) } returns entity

        viewModel.deleteGoal(1L)
        testScheduler.advanceUntilIdle()

        coVerify { sinkingFundRepository.getSinkingFundById(1L) }
        coVerify { sinkingFundRepository.deleteSinkingFund(entity) }
    }
}
