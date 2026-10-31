package com.invoiceforge.app.data.export

import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.domain.model.InvoiceStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testExportClientsToCsv() {
        val clients = listOf(
            ClientEntity(
                id = 1L,
                name = "John Doe",
                company = "Acme Corp",
                phone = "1234567890",
                email = "john@acme.com",
                address = "123 Main St",
                notes = "VIP Client",
                createdAt = 1700000000000L
            )
        )
        val csv = CsvExporter.exportClientsToCsv(clients)

        assertTrue(csv.contains("ID,Name,Company,Phone,Email,Address,Notes,Created Date"))
        assertTrue(csv.contains("\"John Doe\""))
        assertTrue(csv.contains("\"Acme Corp\""))
        assertTrue(csv.contains("\"john@acme.com\""))
    }

    @Test
    fun testExportInvoicesToCsv() {
        val client = ClientEntity(
            id = 1L,
            name = "Jane Smith",
            company = "TechWorks"
        )
        val invoice = InvoiceEntity(
            id = 1L,
            invoiceNumber = "INV-0001",
            clientId = 1L,
            issueDate = 1700000000000L,
            dueDate = 1701000000000L,
            status = InvoiceStatus.PENDING,
            shipping = 15.0
        )
        val items = listOf(
            InvoiceItemEntity(
                id = 1L,
                invoiceId = 1L,
                description = "Web Development",
                quantity = 10.0,
                unitPrice = 50.0
            )
        )
        val invoiceWithDetails = InvoiceWithDetails(
            invoice = invoice,
            client = client,
            items = items,
            payments = emptyList()
        )

        val csv = CsvExporter.exportInvoicesToCsv(listOf(invoiceWithDetails))

        assertTrue(csv.contains("Invoice Number,Client Name,Client Company"))
        assertTrue(csv.contains("\"INV-0001\""))
        assertTrue(csv.contains("\"Jane Smith\""))
        assertTrue(csv.contains("500.00")) // subtotal: 10 * 50 = 500.00
        assertTrue(csv.contains("515.00")) // grand total: 500 + 15 shipping = 515.00
    }
}
