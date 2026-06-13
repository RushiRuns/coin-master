package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.entity.DebtEntity
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.DebtType
import javax.inject.Inject

class GetNetWorthUseCase @Inject constructor() {
    
    /**
     * Calculates the total net worth from the list of accounts and active debts.
     * Credit cards are treated as negative balances.
     * Receivables (LENT) are treated as positive.
     * Payables (BORROWED) are treated as negative.
     */
    operator fun invoke(accounts: List<AccountEntity>, debts: List<DebtEntity> = emptyList()): Long {
        val accountsSum = accounts.sumOf { account ->
            if (account.type == AccountType.CREDIT_CARD) {
                -Math.abs(account.balancePaise)
            } else {
                account.balancePaise
            }
        }
        
        val receivables = debts.filter { it.type == DebtType.LENT && !it.isSettled }.sumOf { it.remainingPaise }
        val payables = debts.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { it.remainingPaise }
        
        return accountsSum + receivables - payables
    }
}
