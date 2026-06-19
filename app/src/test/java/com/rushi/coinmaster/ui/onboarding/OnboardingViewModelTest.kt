package com.rushi.coinmaster.ui.onboarding

import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
import com.rushi.coinmaster.data.repository.IncomeStreamRepository
import com.rushi.coinmaster.domain.usecase.AddTransactionUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val appPreferences: AppPreferences = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val incomeStreamRepository: IncomeStreamRepository = mockk(relaxed = true)
    private val addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true)

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(
            appPreferences,
            accountRepository,
            budgetRepository,
            incomeStreamRepository,
            addTransactionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStep1Validation() {
        // Empty name invalid
        viewModel.userName = ""
        assertFalse(viewModel.validateStep1())

        // Blank name invalid
        viewModel.userName = "   "
        assertFalse(viewModel.validateStep1())

        // Proper name valid
        viewModel.userName = "Rushi"
        assertTrue(viewModel.validateStep1())
    }

    @Test
    fun testStep2Validation() {
        // Empty name invalid
        viewModel.accountName = ""
        viewModel.accountBalanceStr = "100"
        assertFalse(viewModel.validateStep2())

        // Invalid balance format invalid
        viewModel.accountName = "Cash"
        viewModel.accountBalanceStr = "abc"
        assertFalse(viewModel.validateStep2())

        // Negative balance invalid
        viewModel.accountName = "Cash"
        viewModel.accountBalanceStr = "-100"
        assertFalse(viewModel.validateStep2())

        // Correct inputs valid
        viewModel.accountName = "Cash"
        viewModel.accountBalanceStr = "1500.50"
        assertTrue(viewModel.validateStep2())
    }

    @Test
    fun testStep3Validation() {
        // Empty income streams invalid
        assertFalse(viewModel.validateStep3())

        // 0 expected income invalid
        viewModel.addIncomeStream("Zero Stream", 0.0)
        assertFalse(viewModel.validateStep3())

        // Positive income valid
        viewModel.incomeStreams.clear()
        viewModel.addIncomeStream("Salary", 50000.00)
        assertTrue(viewModel.validateStep3())
    }

    @Test
    fun testCompleteOnboarding() = runTest {
        // Set valid inputs
        viewModel.userName = "Rushi"
        viewModel.preferredCurrency = "INR"
        viewModel.accountName = "Primary Bank"
        viewModel.accountType = AccountType.BANK_ACCOUNT
        viewModel.accountBalanceStr = "1000.50"
        
        // Add multiple income streams
        viewModel.addIncomeStream("Salary", 40000.00)
        viewModel.addIncomeStream("Freelance", 10000.00)

        // Mock first account ID return
        coEvery { accountRepository.insertAccount(any()) } returns 1L

        // Mock success collector
        val successEvents = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onboardingSuccess.collect {
                successEvents.add(it)
            }
        }

        // Action
        viewModel.completeOnboarding()
        testScheduler.advanceUntilIdle()

        // Assert preferences set
        coVerify { appPreferences.setUserName("Rushi") }
        coVerify { appPreferences.setPreferredCurrency("INR") }
        coVerify { appPreferences.setOnboardingComplete(true) }

        // Assert database creations
        coVerify { accountRepository.insertAccount(match { 
            it.name == "Primary Bank" && it.type == AccountType.BANK_ACCOUNT && it.balancePaise == 100050L
        }) }
        
        // Assert income streams stored
        coVerify { incomeStreamRepository.insertIncomeStream(match {
            it.name == "Salary" && it.amountPaise == 4000000L && it.accountId == 1L
        }) }
        coVerify { incomeStreamRepository.insertIncomeStream(match {
            it.name == "Freelance" && it.amountPaise == 1000000L && it.accountId == 1L
        }) }

        // Assert income transactions deposited
        coVerify { addTransactionUseCase(match {
            it.amountPaise == 4000000L &&
            it.type == com.rushi.coinmaster.data.local.model.TransactionType.INCOME &&
            it.accountId == 1L &&
            it.note == "Income Stream: Salary"
        }) }
        coVerify { addTransactionUseCase(match {
            it.amountPaise == 1000000L &&
            it.type == com.rushi.coinmaster.data.local.model.TransactionType.INCOME &&
            it.accountId == 1L &&
            it.note == "Income Stream: Freelance"
        }) }

        // Assert budget period created with combined income
        coVerify { budgetRepository.insertBudgetPeriod(match { 
            it.incomePaise == 5000000L && it.needsPercent == 50 && it.wantsPercent == 30 && it.savingsPercent == 20
        }) }

        // Assert success event emitted
        assertEquals(1, successEvents.size)
    }
}
