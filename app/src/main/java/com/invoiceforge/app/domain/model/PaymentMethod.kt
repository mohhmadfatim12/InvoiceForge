package com.invoiceforge.app.domain.model

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    CARD("Card"),
    CHEQUE("Cheque"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}
