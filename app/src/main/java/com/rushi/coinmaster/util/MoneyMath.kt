package com.rushi.coinmaster.util

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyMath {

    /**
     * Converts a Rupee string (e.g. "123.45" or "1,200") to paise (Long).
     * Handles commas, decimal spaces, and invalid formatting gracefully.
     */
    fun rupeesToPaise(amountStr: String): Long {
        val clean = amountStr.replace(",", "").trim()
        if (clean.isEmpty()) return 0L
        return try {
            val bd = BigDecimal(clean)
            // Enforce scaling to 2 decimal places to capture paise properly
            bd.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * Converts a Rupee Double to paise (Long) safely.
     */
    fun rupeesToPaise(amountDouble: Double): Long {
        return try {
            val bd = BigDecimal(amountDouble.toString())
            bd.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Converts paise (Long) back to a raw Double value.
     */
    fun paiseToRupeesDouble(paise: Long): Double {
        return paise.toDouble() / 100.0
    }

    /**
     * Adds multiple paise values together safely.
     */
    fun add(vararg amounts: Long): Long {
        return amounts.sum()
    }

    /**
     * Subtracts b from a.
     */
    fun subtract(a: Long, b: Long): Long {
        return a - b
    }

    /**
     * Calculates the percentage of a paise amount.
     */
    fun percentage(amount: Long, percent: Int): Long {
        if (amount == 0L || percent <= 0) return 0L
        return BigDecimal(amount)
            .multiply(BigDecimal(percent))
            .divide(BigDecimal(100), RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Splits a total income into 50% Needs, 30% Wants, and 20% Savings.
     * Any remainder/rounding difference is allocated to Savings to enforce ZBB.
     */
    fun calculate50_30_20Split(income: Long): Triple<Long, Long, Long> {
        val needs = percentage(income, 50)
        val wants = percentage(income, 30)
        val savings = income - needs - wants // Remainder automatically goes to savings
        return Triple(needs, wants, savings)
    }
}
