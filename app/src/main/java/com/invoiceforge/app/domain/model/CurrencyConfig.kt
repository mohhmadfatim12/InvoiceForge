package com.invoiceforge.app.domain.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyConfig(
    val code: String = "USD",
    val symbol: String = "$",
    val name: String = "US Dollar"
) {
    fun formatAmount(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0.00", symbols)
        return "$symbol${formatter.format(amount)}"
    }

    fun formatAmount(amount: Double): String {
        return formatAmount(BigDecimal.valueOf(amount))
    }

    companion object {
        val DEFAULT = CurrencyConfig("USD", "$", "US Dollar")

        val POPULAR_CURRENCIES = listOf(
            CurrencyConfig("USD", "$", "US Dollar"),
            CurrencyConfig("EUR", "€", "Euro"),
            CurrencyConfig("GBP", "£", "British Pound"),
            CurrencyConfig("PKR", "Rs", "Pakistani Rupee"),
            CurrencyConfig("INR", "₹", "Indian Rupee"),
            CurrencyConfig("CAD", "CA$", "Canadian Dollar"),
            CurrencyConfig("AUD", "AU$", "Australian Dollar"),
            CurrencyConfig("AED", "AED", "UAE Dirham"),
            CurrencyConfig("SAR", "SAR", "Saudi Riyal")
        )

        fun findByCode(code: String): CurrencyConfig {
            return POPULAR_CURRENCIES.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: CurrencyConfig(code, code, code)
        }
    }
}
