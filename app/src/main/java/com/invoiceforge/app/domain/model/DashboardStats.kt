package com.invoiceforge.app.domain.model

import java.math.BigDecimal

data class MonthlyRevenue(
    val monthLabel: String, // e.g. "May", "Jun"
    val invoicedAmount: BigDecimal,
    val receivedAmount: BigDecimal
)

data class DashboardStats(
    val totalInvoiced: BigDecimal = BigDecimal.ZERO,
    val totalReceived: BigDecimal = BigDecimal.ZERO,
    val totalOutstanding: BigDecimal = BigDecimal.ZERO,
    val overdueCount: Int = 0,
    val invoicesThisMonthCount: Int = 0,
    val draftCount: Int = 0,
    val pendingCount: Int = 0,
    val partiallyPaidCount: Int = 0,
    val paidCount: Int = 0,
    val monthlyTrends: List<MonthlyRevenue> = emptyList()
)
