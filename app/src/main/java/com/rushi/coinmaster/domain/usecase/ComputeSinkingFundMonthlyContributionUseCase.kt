package com.rushi.coinmaster.domain.usecase

import java.util.Calendar
import javax.inject.Inject

class ComputeSinkingFundMonthlyContributionUseCase @Inject constructor() {
    
    /**
     * Computes the required monthly savings amount in paise.
     * Enforces integer ceiling division to ensure target is reached.
     */
    operator fun invoke(
        targetAmountPaise: Long,
        savedAmountPaise: Long,
        targetDate: Long, // Epoch millis
        currentDate: Long = System.currentTimeMillis()
    ): Long {
        val remaining = targetAmountPaise - savedAmountPaise
        if (remaining <= 0) return 0L

        val targetCal = Calendar.getInstance().apply { timeInMillis = targetDate }
        val currentCal = Calendar.getInstance().apply { timeInMillis = currentDate }

        val targetYear = targetCal.get(Calendar.YEAR)
        val targetMonth = targetCal.get(Calendar.MONTH) // 0-11
        val currentYear = currentCal.get(Calendar.YEAR)
        val currentMonth = currentCal.get(Calendar.MONTH)

        val monthsRemaining = (targetYear - currentYear) * 12 + (targetMonth - currentMonth)
        if (monthsRemaining <= 0) {
            return remaining
        }

        // Ceiling division: (remaining + monthsRemaining - 1) / monthsRemaining
        return (remaining + monthsRemaining - 1) / monthsRemaining
    }
}
