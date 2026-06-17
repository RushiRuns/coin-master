package com.rushi.coinmaster.ui.income

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.TransactionEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.TransactionType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.domain.model.IncomeStream
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
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
class IncomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val incomeStreamRepository: IncomeStreamRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true)

    private lateinit var viewModel: IncomeViewModel

    private val stream1 = IncomeStream(id = 1L, name = "Salary", amountPaise = 5000000L, accountId = 10L)
    private val stream2 = IncomeStream(id = 2L, name = "Freelance", amountPaise = 1000000L, accountId = 11L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { incomeStreamRepository.getIncomeStreamsFlow() } returns MutableStateFlow(listOf(stream1, stream2))
        every { accountRepository.getAccountsFlow() } returns MutableStateFlow(emptyList())

        viewModel = IncomeViewModel(
            incomeStreamRepository,
            accountRepository,
            addTransactionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `incomeStreams flow filters out deleted streams`() = runTest {
        val deletedStream = IncomeStream(id = 3L, name = "Old Job", amountPaise = 2000000L, accountId = 10L, isDeleted = true)
        every { incomeStreamRepository.getIncomeStreamsFlow() } returns MutableStateFlow(listOf(stream1, stream2, deletedStream))

        // Create new viewmodel to trigger fresh collection of streams flow
        val freshViewModel = IncomeViewModel(incomeStreamRepository, accountRepository, addTransactionUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.incomeStreams.collect {}
        }
        testScheduler.advanceUntilIdle()

        val activeStreams = freshViewModel.incomeStreams.value
        assertEquals(2, activeStreams.size)
        assertTrue(activeStreams.none { it.isDeleted })
    }

    @Test
    fun `totalIncomePaise computes total sum of active streams`() = runTest {
        // stream1 (50000.00) + stream2 (10000.00) = 60000.00 paise (6_000_000 paise)
        val freshViewModel = IncomeViewModel(incomeStreamRepository, accountRepository, addTransactionUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.totalIncomePaise.collect {}
        }
        testScheduler.advanceUntilIdle()

        assertEquals(6_000_000L, freshViewModel.totalIncomePaise.value)
    }

    @Test
    fun `addIncomeStream inserts new stream into repository`() = runTest {
        viewModel.addIncomeStream("Rental", 1500.00, 10L)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            incomeStreamRepository.insertIncomeStream(match {
                it.name == "Rental" && it.amountPaise == 150000L && it.accountId == 10L
            })
        }
    }

    @Test
    fun `deleteIncomeStream soft deletes stream in repository`() = runTest {
        viewModel.deleteIncomeStream(1L)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            incomeStreamRepository.softDeleteIncomeStream(1L)
        }
    }

    @Test
    fun `depositIncomeStream invokes AddTransactionUseCase with INCOME type and correct amount`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns Result.success(1L)

        val events = mutableListOf<IncomeUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.depositIncomeStream(stream1)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            addTransactionUseCase(match {
                it.type == TransactionType.INCOME &&
                it.amountPaise == 5000000L &&
                it.accountId == 10L &&
                it.note == "Income Stream: Salary"
            })
        }

        assertTrue(events.any { it is IncomeUiEvent.ShowToast && it.message == "Deposited Salary successfully!" })
    }

    @Test
    fun `depositIncomeStream fails when no account is linked`() = runTest {
        val unlinkedStream = IncomeStream(id = 4L, name = "Cash Gift", amountPaise = 50000L, accountId = null)

        val events = mutableListOf<IncomeUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.depositIncomeStream(unlinkedStream)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { addTransactionUseCase(any()) }
        assertTrue(events.any { it is IncomeUiEvent.ShowToast && it.message.contains("No account linked") })
    }
}
