package com.invoiceforge.app.domain.model

import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.local.entity.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ClientSummaryCalculationTest {

    @Test
    fun testClientSummaryAggregation() {
        val client = ClientEntity(id = 42L, name = "Starlight Media", company = "Starlight Corp")

        // Invoice 1: $300 total, $100 paid -> $200 balance
        val inv1 = InvoiceEntity(id = 1L, invoiceNumber = "INV-0001", clientId = 42L, issueDate = 1000L, dueDate = 2000L)
        val items1 = listOf(InvoiceItemEntity(id = 1L, invoiceId = 1L, description = "Design", quantity = 3.0, unitPrice = 100.0))
        val pays1 = listOf(PaymentEntity(id = 1L, invoiceId = 1L, amount = 100.0))
        val details1 = InvoiceWithDetails(invoice = inv1, client = client, items = items1, payments = pays1)

        // Invoice 2: $500 total, $500 paid -> $0 balance
        val inv2 = InvoiceEntity(id = 2L, invoiceNumber = "INV-0002", clientId = 42L, issueDate = 1000L, dueDate = 2000L)
        val items2 = listOf(InvoiceItemEntity(id = 2L, invoiceId = 2L, description = "Hosting", quantity = 1.0, unitPrice = 500.0))
        val pays2 = listOf(PaymentEntity(id = 2L, invoiceId = 2L, amount = 500.0))
        val details2 = InvoiceWithDetails(invoice = inv2, client = client, items = items2, payments = pays2)

        val invoices = listOf(details1, details2)

        var totalInvoiced = BigDecimal.ZERO
        var totalPaid = BigDecimal.ZERO

        invoices.forEach { item ->
            val calc = item.calculate()
            totalInvoiced = totalInvoiced.add(calc.grandTotal)
            totalPaid = totalPaid.add(calc.amountPaid)
        }

        val outstanding = totalInvoiced.subtract(totalPaid).max(BigDecimal.ZERO)

        val summary = ClientSummary(
            clientId = client.id,
            clientName = client.name,
            company = client.company,
            totalInvoices = invoices.size,
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            outstandingAmount = outstanding
        )

        assertEquals(2, summary.totalInvoices)
        assertEquals(BigDecimal("800.00"), summary.totalInvoiced)
        assertEquals(BigDecimal("600.00"), summary.totalPaid)
        assertEquals(BigDecimal("200.00"), summary.outstandingAmount)
    }
}
