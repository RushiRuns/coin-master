package com.rushi.coinmaster.domain.usecase

import com.rushi.coinmaster.data.local.entity.AccountEntity
import com.rushi.coinmaster.data.local.model.AccountType
import javax.inject.Inject

class GetNetWorthUseCase @Inject constructor() {
    
    /**
     * Calculates the total net worth from the list of accounts.
     * Credit cards are treated as negative balances.
     */
    operator fun invoke(accounts: List<AccountEntity>): Long {
        return accounts.sumOf { account ->
            if (account.type == AccountType.CREDIT_CARD) {
                -Math.abs(account.balancePaise)
            } else {
                account.balancePaise
            }
        }
    }
}
