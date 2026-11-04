package com.example

import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `test FormatUtils formatAmount avoids scientific notation for 0_0001`() {
        val amount = 0.0001
        val formatted = FormatUtils.formatAmount(amount)
        assertEquals("0.0001", formatted)
        assertFalse(formatted.contains("E", ignoreCase = true))
    }

    @Test
    fun `test FormatUtils formatAmount for 5000_0`() {
        val amount = 5000.0
        val formatted = FormatUtils.formatAmount(amount)
        assertEquals("5000", formatted)
        assertFalse(formatted.contains("E", ignoreCase = true))
    }

    @Test
    fun `test FormatUtils formatAmount for micro values`() {
        val micro = 0.00000025
        val formatted = FormatUtils.formatAmount(micro)
        assertEquals("0.00000025", formatted)
        assertFalse(formatted.contains("E", ignoreCase = true))
    }

    @Test
    fun `test FormatUtils formatPrice for standard and crypto prices`() {
        assertEquals("1,010.00", FormatUtils.formatPrice(1010.0))
        assertEquals("1,000.00", FormatUtils.formatPrice(1000.0))
        assertEquals("0.0001", FormatUtils.formatPrice(0.0001))
        assertFalse(FormatUtils.formatPrice(0.0001).contains("E", ignoreCase = true))
    }

    @Test
    fun `test FormatUtils formatPercent`() {
        assertEquals("1", FormatUtils.formatPercent(1.0))
        assertEquals("2.5", FormatUtils.formatPercent(2.5))
    }

    @Test
    fun `test FormatUtils sanitizeCurrency removes all dollar signs`() {
        assertEquals("", FormatUtils.sanitizeCurrency("\$"))
        assertEquals("", FormatUtils.sanitizeCurrency("\$\$\$"))
        assertEquals("USDT", FormatUtils.sanitizeCurrency("\$USDT"))
        assertEquals("", FormatUtils.sanitizeCurrency(null))
        assertEquals("₺", FormatUtils.sanitizeCurrency("₺"))
    }

    @Test
    fun `test FormatUtils prefixCurrency never contains dollar sign`() {
        val formatted = FormatUtils.prefixCurrency("1,010.00", "$")
        assertEquals("1,010.00", formatted)
        assertFalse(formatted.contains("$"))

        val formattedEmpty = FormatUtils.prefixCurrency("250.00", "")
        assertEquals("250.00", formattedEmpty)
        assertFalse(formattedEmpty.contains("$"))

        val formattedUsdt = FormatUtils.prefixCurrency("1,010.00", "USDT")
        assertEquals("USDT 1,010.00", formattedUsdt)
        assertFalse(formattedUsdt.contains("$"))

        val formattedTry = FormatUtils.prefixCurrency("1,010.00", "₺")
        assertEquals("₺1,010.00", formattedTry)
        assertFalse(formattedTry.contains("$"))
    }
}
