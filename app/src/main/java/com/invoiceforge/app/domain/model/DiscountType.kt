package com.invoiceforge.app.domain.model

enum class DiscountType {
    PERCENTAGE,
    FIXED;

    companion object {
        fun fromString(value: String?): DiscountType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PERCENTAGE
        }
    }
}
