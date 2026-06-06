package com.rushi.coinmaster.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    /**
     * Formats paise (Long) to a readable currency string based on the locale language.
     * Uses Indian grouping (Lakhs/Crores) for Hindi ("hi") and Marathi ("mr") locales.
     * Uses standard Western grouping for English ("en").
     */
    fun format(paise: Long, languageCode: String, showPaise: Boolean = false): String {
        val sign = if (paise < 0) "-" else ""
        val absolutePaise = Math.abs(paise)
        val rupees = absolutePaise / 100
        val paiseFraction = absolutePaise % 100

        val formattedRupees = if (languageCode == "hi" || languageCode == "mr") {
            formatIndianGroup(rupees)
        } else {
            formatWesternGroup(rupees)
        }

        val paisePart = if (showPaise) {
            val paiseStr = paiseFraction.toString().padStart(2, '0')
            ".$paiseStr"
        } else {
            ""
        }

        return "${sign}₹$formattedRupees$paisePart"
    }

    private fun formatIndianGroup(rupees: Long): String {
        val s = rupees.toString()
        val len = s.length
        if (len <= 3) return s

        val lastThree = s.substring(len - 3)
        val remaining = s.substring(0, len - 3)

        val grouped = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            grouped.append(remaining[i])
            count++
            if (count == 2 && i > 0) {
                grouped.append(',')
                count = 0
            }
        }
        return "${grouped.reverse()},$lastThree"
    }

    private fun formatWesternGroup(rupees: Long): String {
        val s = rupees.toString()
        val len = s.length
        if (len <= 3) return s

        val grouped = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            grouped.append(s[i])
            count++
            if (count == 3 && i > 0) {
                grouped.append(',')
                count = 0
            }
        }
        return grouped.reverse().toString()
    }
}

object DateFormatter {

    private const val DATE_PATTERN_DEFAULT = "dd MMM yyyy"
    private const val DATE_PATTERN_MONTH_YEAR = "MMMM yyyy"

    /**
     * Formats epoch millis to a standard date string: "dd MMM yyyy" (e.g. 06 Jun 2026).
     */
    fun formatDate(epochMillis: Long, languageCode: String = "en"): String {
        val locale = Locale(languageCode)
        val sdf = SimpleDateFormat(DATE_PATTERN_DEFAULT, locale)
        return sdf.format(Date(epochMillis))
    }

    /**
     * Formats epoch millis to month and year string: "MMMM yyyy" (e.g. June 2026).
     */
    fun formatMonthYear(epochMillis: Long, languageCode: String = "en"): String {
        val locale = Locale(languageCode)
        val sdf = SimpleDateFormat(DATE_PATTERN_MONTH_YEAR, locale)
        return sdf.format(Date(epochMillis))
    }
}
