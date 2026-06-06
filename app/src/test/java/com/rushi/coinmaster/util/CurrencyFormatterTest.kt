package com.rushi.coinmaster.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class CurrencyFormatterTest {

    @Test
    fun testCurrencyFormatting_IndianLocales() {
        // ₹1,000.00 -> 100,000 paise
        assertEquals("₹1,000", CurrencyFormatter.format(100000L, "hi"))
        assertEquals("₹1,000", CurrencyFormatter.format(100000L, "mr"))

        // ₹1,00,000.00 -> 10,000,000 paise (1 Lakh)
        assertEquals("₹1,00,000", CurrencyFormatter.format(10000000L, "hi"))
        assertEquals("₹1,00,000", CurrencyFormatter.format(10000000L, "mr"))

        // ₹1,00,00,000.00 -> 1,000,000,000 paise (1 Crore)
        assertEquals("₹1,00,00,000", CurrencyFormatter.format(1000000000L, "hi"))
        assertEquals("₹1,00,00,000", CurrencyFormatter.format(1000000000L, "mr"))

        // Negative values
        assertEquals("-₹1,00,000", CurrencyFormatter.format(-10000000L, "hi"))

        // With paise shown
        assertEquals("₹1,00,000.50", CurrencyFormatter.format(10000050L, "hi", showPaise = true))
    }

    @Test
    fun testCurrencyFormatting_WesternLocale() {
        // English uses standard Western formatting
        assertEquals("₹1,000", CurrencyFormatter.format(100000L, "en"))
        assertEquals("₹100,000", CurrencyFormatter.format(10000000L, "en"))
        assertEquals("₹10,000,000", CurrencyFormatter.format(1000000000L, "en"))
        assertEquals("-₹100,000", CurrencyFormatter.format(-10000000L, "en"))
        assertEquals("₹100,000.50", CurrencyFormatter.format(10000050L, "en", showPaise = true))
    }

    @Test
    fun testDateFormatting() {
        // 1780700000000L -> Mon Jun 08 2026 09:33:20 UTC
        val epochMillis = 1780700000000L
        val localTz = TimeZone.getDefault()

        val expectedDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = localTz
        }.format(Date(epochMillis))

        val expectedMonthYear = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).apply {
            timeZone = localTz
        }.format(Date(epochMillis))

        assertEquals(expectedDate, DateFormatter.formatDate(epochMillis, "en"))
        assertEquals(expectedMonthYear, DateFormatter.formatMonthYear(epochMillis, "en"))
    }
}
