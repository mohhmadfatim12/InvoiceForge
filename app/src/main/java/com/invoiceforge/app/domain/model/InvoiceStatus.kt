package com.invoiceforge.app.domain.model

enum class InvoiceStatus(val displayName: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid"),
    OVERDUE("Overdue");

    companion object {
        fun fromString(value: String?): InvoiceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DRAFT
        }
    }
}
