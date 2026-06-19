package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.DebtType
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNetWorthUseCaseTest {

    private val getNetWorthUseCase = GetNetWorthUseCase()

    @Test
    fun testNetWorthCalculation() {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, balancePaise = 10000L, colorHex = "", iconName = ""),
            AccountEntity(id = 2L, name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 50000L, colorHex = "", iconName = ""),
            // Credit card liability is treated as negative asset
            AccountEntity(id = 3L, name = "Credit Card", type = AccountType.CREDIT_CARD, balancePaise = 20000L, colorHex = "", iconName = ""),
            AccountEntity(id = 4L, name = "Mutual Funds", type = AccountType.INVESTMENTS, balancePaise = 30000L, colorHex = "", iconName = "")
        )
        
        // Expected Net Worth: 10000 + 50000 - 20000 + 30000 = 70000 paise
        val netWorth = getNetWorthUseCase(accounts)
        assertEquals(70000L, netWorth)
    }

    @Test
    fun testNetWorthCalculationWithDebts() {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, balancePaise = 10000L, colorHex = "", iconName = ""),
            AccountEntity(id = 2L, name = "Bank", type = AccountType.BANK_ACCOUNT, balancePaise = 50000L, colorHex = "", iconName = "")
        )
        
        val debts = listOf(
            // Receivables (+₹20.00 / 2000 paise)
            DebtEntity(id = 1L, personName = "Sunita", type = DebtType.LENT, amountPaise = 3000L, remainingPaise = 2000L, isSettled = false, date = 0L),
            // Payables (-₹10.00 / 1000 paise)
            DebtEntity(id = 2L, personName = "Rohit", type = DebtType.BORROWED, amountPaise = 1000L, remainingPaise = 1000L, isSettled = false, date = 0L),
            // Settled debts should be ignored
            DebtEntity(id = 3L, personName = "Amit", type = DebtType.LENT, amountPaise = 5000L, remainingPaise = 0L, isSettled = true, date = 0L)
        )

        // Expected Net Worth: Accounts (10000 + 50000) + Receivables (2000) - Payables (1000) = 61000 paise
        val netWorth = getNetWorthUseCase(accounts, debts)
        assertEquals(61000L, netWorth)
    }

    @Test
    fun testEmptyAccountsYieldZero() {
        val netWorth = getNetWorthUseCase(emptyList())
        assertEquals(0L, netWorth)
    }
}
