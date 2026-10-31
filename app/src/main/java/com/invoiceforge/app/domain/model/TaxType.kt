package com.invoiceforge.app.domain.model

enum class TaxType {
    PERCENTAGE,
    FIXED;

    companion object {
        fun fromString(value: String?): TaxType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PERCENTAGE
        }
    }
}
