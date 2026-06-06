package com.rushi.coinmaster.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyMathTest {

    @Test
    fun testRupeesToPaise_fromString() {
        assertEquals(10050L, MoneyMath.rupeesToPaise("100.50"))
        assertEquals(10050L, MoneyMath.rupeesToPaise("100.5"))
        assertEquals(100000L, MoneyMath.rupeesToPaise("1000"))
        assertEquals(120075L, MoneyMath.rupeesToPaise("1,200.75"))
        assertEquals(0L, MoneyMath.rupeesToPaise(""))
        assertEquals(0L, MoneyMath.rupeesToPaise("invalid"))
    }

    @Test
    fun testRupeesToPaise_fromDouble() {
        assertEquals(10050L, MoneyMath.rupeesToPaise(100.50))
        assertEquals(10050L, MoneyMath.rupeesToPaise(100.5))
        assertEquals(100000L, MoneyMath.rupeesToPaise(1000.0))
    }

    @Test
    fun testPaiseToRupeesDouble() {
        assertEquals(100.50, MoneyMath.paiseToRupeesDouble(10050L), 0.0)
        assertEquals(1000.0, MoneyMath.paiseToRupeesDouble(100000L), 0.0)
    }

    @Test
    fun testBasicArithmetic() {
        assertEquals(15000L, MoneyMath.add(10000L, 5000L))
        assertEquals(5000L, MoneyMath.subtract(10000L, 5000L))
    }

    @Test
    fun testPercentage() {
        assertEquals(5000L, MoneyMath.percentage(10000L, 50))
        assertEquals(3000L, MoneyMath.percentage(10000L, 30))
        assertEquals(2000L, MoneyMath.percentage(10000L, 20))
        
        // Test rounding half up
        // 100005 * 50 / 100 = 50002.5 -> 50003
        assertEquals(50003L, MoneyMath.percentage(100005L, 50))
    }

    @Test
    fun testBudgetSplit() {
        // Safe split
        val (needs, wants, savings) = MoneyMath.calculate50_30_20Split(100000L) // ₹1000.00
        assertEquals(50000L, needs)
        assertEquals(30000L, wants)
        assertEquals(20000L, savings)
        assertEquals(100000L, needs + wants + savings)

        // Split with rounding remainder: ₹1000.05
        val (n2, w2, s2) = MoneyMath.calculate50_30_20Split(100005L)
        // 100005 * 50% = 50002.5 -> 50003
        // 100005 * 30% = 30001.5 -> 30002
        // Remainder goes to savings: 100005 - 50003 - 30002 = 19999
        assertEquals(50003L, n2)
        assertEquals(30002L, w2)
        assertEquals(20000L, s2)
        assertEquals(100005L, n2 + w2 + s2) // Sum is preserved
    }
}
