package com.invoiceforge.app.domain.model

import java.math.BigDecimal

data class ClientSummary(
    val clientId: Long,
    val clientName: String,
    val company: String,
    val totalInvoices: Int = 0,
    val totalInvoiced: BigDecimal = BigDecimal.ZERO,
    val totalPaid: BigDecimal = BigDecimal.ZERO,
    val outstandingAmount: BigDecimal = BigDecimal.ZERO
)
