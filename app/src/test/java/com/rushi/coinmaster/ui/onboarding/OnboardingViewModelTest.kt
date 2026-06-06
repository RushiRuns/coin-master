package com.rushi.coinmaster.ui.onboarding

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.BudgetMonthEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.data.repository.BudgetRepository
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

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(appPreferences, accountRepository, budgetRepository)
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
        // Invalid income format invalid
        viewModel.monthlyIncomeStr = "abc"
        assertFalse(viewModel.validateStep3())

        // Negative income invalid
        viewModel.monthlyIncomeStr = "-50000"
        assertFalse(viewModel.validateStep3())

        // Zero income invalid
        viewModel.monthlyIncomeStr = "0"
        assertFalse(viewModel.validateStep3())

        // Correct income valid
        viewModel.monthlyIncomeStr = "50000.00"
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
        viewModel.monthlyIncomeStr = "50000.00"

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
        coVerify { budgetRepository.seedDefaultCategories() }
        coVerify { budgetRepository.insertBudgetMonth(match { 
            it.incomePaise == 5000000L && it.needsPercent == 50 && it.wantsPercent == 30 && it.savingsPercent == 20
        }) }

        // Assert success event emitted
        assertEquals(1, successEvents.size)
    }
}
