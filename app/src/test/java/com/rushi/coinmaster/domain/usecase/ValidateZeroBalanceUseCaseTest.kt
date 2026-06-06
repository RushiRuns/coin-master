package com.rushi.coinmaster.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateZeroBalanceUseCaseTest {

    private val validateZeroBalanceUseCase = ValidateZeroBalanceUseCase()

    @Test
    fun testBalancedBudget() {
        val income = 50000_00L // ₹50,000
        val allocations = listOf(
            25000_00L, // Needs: ₹25,000
            15000_00L, // Wants: ₹15,000
            10000_00L  // Savings: ₹10,000
        )
        
        val result = validateZeroBalanceUseCase(income, allocations)
        assertTrue(result.isValid)
        assertEquals(0L, result.differencePaise)
    }

    @Test
    fun testSurplusBudget() {
        val income = 50000_00L // ₹50,000
        val allocations = listOf(
            20000_00L, // Needs: ₹20,000
            15000_00L, // Wants: ₹15,000
            10000_00L  // Savings: ₹10,000
        )
        
        val result = validateZeroBalanceUseCase(income, allocations)
        assertFalse(result.isValid)
        assertEquals(5000_00L, result.differencePaise) // ₹5,000 unallocated surplus
    }

    @Test
    fun testDeficitBudget() {
        val income = 50000_00L // ₹50,000
        val allocations = listOf(
            30000_00L, // Needs: ₹30,000
            15000_00L, // Wants: ₹15,000
            10000_00L  // Savings: ₹10,000
        )
        
        val result = validateZeroBalanceUseCase(income, allocations)
        assertFalse(result.isValid)
        assertEquals(-5000_00L, result.differencePaise) // ₹5,000 over-allocated deficit
    }

    @Test
    fun testZeroIncomeAndZeroAllocations() {
        val result = validateZeroBalanceUseCase(0L, emptyList())
        assertTrue(result.isValid)
        assertEquals(0L, result.differencePaise)
    }
}
