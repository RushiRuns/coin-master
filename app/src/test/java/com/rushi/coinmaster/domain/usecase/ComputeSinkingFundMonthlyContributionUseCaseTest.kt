package com.rushi.coinmaster.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ComputeSinkingFundMonthlyContributionUseCaseTest {

    private val useCase = ComputeSinkingFundMonthlyContributionUseCase()

    private fun getEpochMillis(year: Int, month: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }

    @Test
    fun testGoalAlreadyReachedReturnsZero() {
        val targetAmount = 10000L
        val savedAmount = 10000L
        val targetDate = getEpochMillis(2027, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(0L, contribution)
    }

    @Test
    fun testGoalOverreachedReturnsZero() {
        val targetAmount = 10000L
        val savedAmount = 12000L
        val targetDate = getEpochMillis(2027, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(0L, contribution)
    }

    @Test
    fun testCurrentMonthTargetReturnsRemaining() {
        val targetAmount = 10000L
        val savedAmount = 2000L
        val targetDate = getEpochMillis(2026, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(8000L, contribution)
    }

    @Test
    fun testPastMonthTargetReturnsRemaining() {
        val targetAmount = 10000L
        val savedAmount = 2000L
        val targetDate = getEpochMillis(2025, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(8000L, contribution)
    }

    @Test
    fun testFutureTargetDividesCorrectly() {
        // ₹12,000 target, ₹0 saved, 12 months away (June 2026 -> June 2027)
        val targetAmount = 12000_00L
        val savedAmount = 0L
        val targetDate = getEpochMillis(2027, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(1000_00L, contribution) // ₹1,000 per month
    }

    @Test
    fun testFutureTargetWithSavedAmountDividesCorrectly() {
        // ₹12,000 target, ₹4,000 saved, 8 months away (Oct 2026 -> June 2027)
        val targetAmount = 12000_00L
        val savedAmount = 4000_00L
        val targetDate = getEpochMillis(2027, Calendar.JUNE)
        val currentDate = getEpochMillis(2026, Calendar.OCTOBER)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        assertEquals(1000_00L, contribution) // ₹8,000 left / 8 months = ₹1,000 per month
    }

    @Test
    fun testCeilingDivisionRoundsUp() {
        // Remaining = 10001 paise, 3 months remaining
        val targetAmount = 10001L
        val savedAmount = 0L
        val targetDate = getEpochMillis(2026, Calendar.SEPTEMBER)
        val currentDate = getEpochMillis(2026, Calendar.JUNE)

        val contribution = useCase(targetAmount, savedAmount, targetDate, currentDate)
        // 10001 / 3 = 3333.666 -> rounds up to 3334
        assertEquals(3334L, contribution)
    }
}
