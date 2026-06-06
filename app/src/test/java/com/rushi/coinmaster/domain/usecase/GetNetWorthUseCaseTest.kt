package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
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
    fun testEmptyAccountsYieldZero() {
        val netWorth = getNetWorthUseCase(emptyList())
        assertEquals(0L, netWorth)
    }
}
