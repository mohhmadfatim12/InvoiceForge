package com.invoiceforge.app.data.export

import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private fun escape(value: String?): String {
        if (value == null) return "\"\""
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun exportInvoicesToCsv(invoices: List<InvoiceWithDetails>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.append("Invoice Number,Client Name,Client Company,Issue Date,Due Date,Status,Subtotal,Discount Amount,Tax Amount,Shipping,Grand Total,Amount Paid,Balance Due,Notes\n")

        for (item in invoices) {
            val invoice = item.invoice
            val client = item.client
            val calc = item.calculate()

            sb.append(escape(invoice.invoiceNumber)).append(",")
            sb.append(escape(client?.name.orEmpty())).append(",")
            sb.append(escape(client?.company.orEmpty())).append(",")
            sb.append(escape(dateFormat.format(Date(invoice.issueDate)))).append(",")
            sb.append(escape(dateFormat.format(Date(invoice.dueDate)))).append(",")
            sb.append(escape(calc.status.displayName)).append(",")
            sb.append(calc.subtotal).append(",")
            sb.append(calc.discountAmount).append(",")
            sb.append(calc.taxAmount).append(",")
            sb.append(invoice.shipping).append(",")
            sb.append(calc.grandTotal).append(",")
            sb.append(calc.amountPaid).append(",")
            sb.append(calc.balanceDue).append(",")
            sb.append(escape(invoice.notes))
            sb.append("\n")
        }
        return sb.toString()
    }

    fun exportClientsToCsv(clients: List<ClientEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Name,Company,Phone,Email,Address,Notes,Created Date\n")

        for (client in clients) {
            sb.append(client.id).append(",")
            sb.append(escape(client.name)).append(",")
            sb.append(escape(client.company)).append(",")
            sb.append(escape(client.phone)).append(",")
            sb.append(escape(client.email)).append(",")
            sb.append(escape(client.address)).append(",")
            sb.append(escape(client.notes)).append(",")
            sb.append(escape(dateFormat.format(Date(client.createdAt))))
            sb.append("\n")
        }
        return sb.toString()
    }
}
