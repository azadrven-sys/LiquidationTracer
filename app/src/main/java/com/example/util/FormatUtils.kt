package com.example.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object FormatUtils {

    /**
     * Converts any Double to a plain string with NO scientific notation (no 'E' or 'e').
     * Example: 0.0001 -> "0.0001", 5000.0 -> "5000", 0.0 -> "0".
     */
    fun formatPlain(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        if (value == 0.0) return "0"
        return try {
            BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        } catch (_: Exception) {
            "0"
        }
    }

    /**
     * Formats an amount / quantity (Miktar).
     * Strictly avoids scientific notation (e.g. 0.0001 instead of 1.0E-4, 5000 instead of 5000.0).
     */
    fun formatAmount(value: Double): String {
        return formatPlain(value)
    }

    /**
     * Formats price without scientific notation.
     * Values >= 1.0: standard 2 decimal places with thousands separator (e.g. 1,010.00).
     * Values < 1.0: up to 8 decimal places without trailing zeros (e.g. 0.0001, 0.000054).
     */
    fun formatPrice(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0.00"
        if (value == 0.0) return "0.00"

        val absVal = abs(value)
        return try {
            val symbols = DecimalFormatSymbols(Locale.US).apply {
                decimalSeparator = '.'
                groupingSeparator = ','
            }
            if (absVal >= 1.0) {
                val df = DecimalFormat("#,##0.00", symbols)
                df.format(value)
            } else {
                val df = DecimalFormat("0.########", symbols)
                val res = df.format(value)
                if (res == "0" || res == "-0") {
                    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
                } else {
                    res
                }
            }
        } catch (_: Exception) {
            formatPlain(value)
        }
    }

    /**
     * Formats monetary totals (Cost, Profit, PnL).
     * Values >= 1.0 or == 0.0: standard 2 decimal places (e.g. 150.00).
     * Values < 1.0: dynamic precision up to 6 decimal places (e.g. 0.0005).
     * Never scientific notation.
     */
    fun formatCurrency(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0.00"
        if (value == 0.0) return "0.00"

        val absVal = abs(value)
        return try {
            val symbols = DecimalFormatSymbols(Locale.US).apply {
                decimalSeparator = '.'
                groupingSeparator = ','
            }
            if (absVal >= 1.0) {
                val df = DecimalFormat("#,##0.00", symbols)
                df.format(value)
            } else {
                val df = DecimalFormat("0.######", symbols)
                val res = df.format(value)
                if (res == "0" || res == "-0") {
                    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
                } else {
                    res
                }
            }
        } catch (_: Exception) {
            formatPlain(value)
        }
    }

    /**
     * Formats target percentage cleanly (e.g. 1.0 -> "1", 2.5 -> "2.5").
     */
    fun formatPercent(value: Double): String {
        return formatPlain(value)
    }

    /**
     * Sanitizes any currency string, ensuring dollar signs ($) are NEVER displayed anywhere.
     */
    fun sanitizeCurrency(symbol: String?): String {
        if (symbol == null) return ""
        return symbol.replace("$", "").trim()
    }

    /**
     * Prefixes or formats currency, guaranteeing no dollar sign ($) is ever displayed.
     * When symbol is empty or was '$', returns just the cleanly formatted value.
     */
    fun prefixCurrency(formattedValue: String, symbol: String?): String {
        val cleanSym = sanitizeCurrency(symbol)
        return when {
            cleanSym.isEmpty() -> formattedValue
            cleanSym.length == 1 -> "$cleanSym$formattedValue"
            else -> "$cleanSym $formattedValue"
        }
    }
}
