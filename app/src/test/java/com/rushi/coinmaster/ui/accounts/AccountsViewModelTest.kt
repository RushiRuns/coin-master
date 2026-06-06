package com.rushi.coinmaster.ui.accounts

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.repository.AccountRepository
import com.rushi.coinmaster.domain.usecase.GetNetWorthUseCase
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
class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val getNetWorthUseCase: GetNetWorthUseCase = mockk()

    private lateinit var viewModel: AccountsViewModel

    private val testAccounts = listOf(
        AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, balancePaise = 10000L, colorHex = "#000", iconName = "ic_cash"),
        AccountEntity(id = 2L, name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 50000L, colorHex = "#FFF", iconName = "ic_bank")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { accountRepository.getAccountsFlow() } returns flowOf(testAccounts)
        every { getNetWorthUseCase(testAccounts) } returns 60000L
        
        viewModel = AccountsViewModel(accountRepository, getNetWorthUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiStateEmitsCorrectData() = runTest {
        val states = mutableListOf<AccountsUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {
                states.add(it)
            }
        }

        testScheduler.advanceUntilIdle()

        val lastState = states.last()
        assertEquals(testAccounts, lastState.accounts)
        assertEquals(60000L, lastState.netWorth)
    }

    @Test
    fun testSaveNewAccount() = runTest {
        viewModel.saveAccount(
            id = 0L,
            name = "New Savings",
            type = AccountType.BANK_ACCOUNT,
            balanceStr = "1500.50",
            colorHex = "#123",
            iconName = "ic_bank"
        )
        testScheduler.advanceUntilIdle()

        coVerify { accountRepository.insertAccount(match {
            it.name == "New Savings" && it.type == AccountType.BANK_ACCOUNT && it.balancePaise == 150050L && it.colorHex == "#123" && it.iconName == "ic_bank"
        }) }
    }

    @Test
    fun testSaveExistingAccountPreservesBalance() = runTest {
        val existing = AccountEntity(id = 5L, name = "Old Name", type = AccountType.CASH, balancePaise = 99999L, colorHex = "#FFF", iconName = "ic_cash")
        coEvery { accountRepository.getAccountById(5L) } returns existing

        viewModel.saveAccount(
            id = 5L,
            name = "Updated Name",
            type = AccountType.CASH,
            balanceStr = "ignored_balance",
            colorHex = "#000",
            iconName = "ic_new_cash"
        )
        testScheduler.advanceUntilIdle()

        coVerify { accountRepository.updateAccount(match {
            it.id == 5L && it.name == "Updated Name" && it.balancePaise == 99999L && it.colorHex == "#000" && it.iconName == "ic_new_cash"
        }) }
    }

    @Test
    fun testDeleteAccount() = runTest {
        val account = testAccounts[0]
        viewModel.deleteAccount(account)
        testScheduler.advanceUntilIdle()

        coVerify { accountRepository.softDeleteAccount(account.id) }
    }

    @Test
    fun testRestoreAccount() = runTest {
        val account = testAccounts[0].copy(isDeleted = true)
        viewModel.restoreAccount(account)
        testScheduler.advanceUntilIdle()

        coVerify { accountRepository.updateAccount(match {
            it.id == account.id && !it.isDeleted
        }) }
    }
}
