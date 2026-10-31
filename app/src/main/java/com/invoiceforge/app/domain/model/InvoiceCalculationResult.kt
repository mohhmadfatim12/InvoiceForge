package com.invoiceforge.app.domain.model

import java.math.BigDecimal

data class InvoiceCalculationResult(
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val grandTotal: BigDecimal,
    val amountPaid: BigDecimal,
    val balanceDue: BigDecimal,
    val status: InvoiceStatus,
    val isOverdue: Boolean
)
