package com.rushi.coinmaster.domain.usecase

import javax.inject.Inject

class ValidateZeroBalanceUseCase @Inject constructor() {
    operator fun invoke(incomePaise: Long, allocatedAmounts: List<Long>): BudgetValidationResult {
        val totalAllocated = allocatedAmounts.sum()
        val difference = incomePaise - totalAllocated
        return BudgetValidationResult(
            isValid = difference == 0L,
            differencePaise = difference
        )
    }
}

data class BudgetValidationResult(
    val isValid: Boolean,
    val differencePaise: Long // positive = surplus (unallocated), negative = deficit (over-allocated)
)
