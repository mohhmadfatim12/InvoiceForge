package com.invoiceforge.app.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.domain.calculator.InvoiceCalculator
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.TaxType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfInvoiceGenerator(private val context: Context) {

    companion object {
        const val PAGE_WIDTH = 595 // Standard A4 width in points
        const val PAGE_HEIGHT = 842 // Standard A4 height in points
        const val MARGIN = 40f
    }

    fun generateInvoicePdf(
        invoiceWithDetails: InvoiceWithDetails,
        businessProfile: BusinessProfileEntity?
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoice(canvas, invoiceWithDetails, businessProfile)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "pdf_cache")
        if (!outputDir.exists()) outputDir.mkdirs()

        val fileName = "Invoice_${invoiceWithDetails.invoice.invoiceNumber.replace("[^a-zA-Z0-9-]".toRegex(), "_")}.pdf"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawInvoice(
        canvas: Canvas,
        invoiceWithDetails: InvoiceWithDetails,
        businessProfile: BusinessProfileEntity?
    ) {
        val invoice = invoiceWithDetails.invoice
        val client = invoiceWithDetails.client
        val items = invoiceWithDetails.items
        val payments = invoiceWithDetails.payments
        val calc = invoiceWithDetails.calculate()
        val currency = CurrencyConfig.findByCode(businessProfile?.currency ?: "USD")

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(24, 76, 120) // Deep Navy Accent
            isAntiAlias = true
        }

        val textPrimaryPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textSecondaryPaint = Paint().apply {
            color = Color.rgb(117, 117, 117)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(24, 76, 120)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(224, 224, 224)
            strokeWidth = 1f
            isAntiAlias = true
        }

        // Top decorative bar
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, primaryPaint)

        var y = 45f

        // Header: Business Profile on left
        val businessName = businessProfile?.name?.ifBlank { "InvoiceForge Studio" } ?: "InvoiceForge Studio"
        titlePaint.textSize = 18f
        canvas.drawText(businessName, MARGIN, y, titlePaint)
        y += 14f

        if (!businessProfile?.address.isNullOrBlank()) {
            canvas.drawText(businessProfile!!.address, MARGIN, y, textSecondaryPaint)
            y += 12f
        }
        val contactText = listOfNotNull(
            businessProfile?.phone?.takeIf { it.isNotBlank() },
            businessProfile?.email?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        if (contactText.isNotBlank()) {
            canvas.drawText(contactText, MARGIN, y, textSecondaryPaint)
            y += 12f
        }

        // Right side: INVOICE label and metadata
        val rightX = PAGE_WIDTH - MARGIN
        val invoiceTitlePaint = Paint(titlePaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 24f
            color = Color.rgb(24, 76, 120)
        }
        canvas.drawText("INVOICE", rightX, 45f, invoiceTitlePaint)

        val metaPaint = Paint(textPrimaryPaint).apply { textAlign = Paint.Align.RIGHT }
        val metaLabelPaint = Paint(textSecondaryPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("Invoice #: ${invoice.invoiceNumber}", rightX, 65f, metaPaint)
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.issueDate))}", rightX, 78f, metaLabelPaint)
        canvas.drawText("Due: ${dateFormat.format(Date(invoice.dueDate))}", rightX, 91f, metaLabelPaint)

        // Status Badge
        val statusText = calc.status.displayName.uppercase()
        val statusPaint = Paint().apply {
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val badgeColor = when (calc.status.name) {
            "PAID" -> Color.rgb(46, 125, 50) // Green
            "PARTIALLY_PAID" -> Color.rgb(230, 81, 0) // Orange
            "OVERDUE" -> Color.rgb(198, 40, 40) // Red
            else -> Color.rgb(30, 136, 229) // Blue
        }
        statusPaint.color = badgeColor
        canvas.drawText("Status: $statusText", rightX, 105f, statusPaint)

        y = maxOf(y + 15f, 125f)

        // Divider
        canvas.drawLine(MARGIN, y, rightX, y, linePaint)
        y += 15f

        // Bill To section
        canvas.drawText("BILLED TO", MARGIN, y, sectionTitlePaint)
        y += 13f

        val clientName = client?.name?.ifBlank { "Guest Client" } ?: "No Client Specified"
        val clientPaint = Paint(textPrimaryPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(clientName, MARGIN, y, clientPaint)
        y += 12f

        if (!client?.company.isNullOrBlank()) {
            canvas.drawText(client!!.company, MARGIN, y, textSecondaryPaint)
            y += 11f
        }
        if (!client?.email.isNullOrBlank()) {
            canvas.drawText(client!!.email, MARGIN, y, textSecondaryPaint)
            y += 11f
        }
        if (!client?.phone.isNullOrBlank()) {
            canvas.drawText(client!!.phone, MARGIN, y, textSecondaryPaint)
            y += 11f
        }
        if (!client?.address.isNullOrBlank()) {
            canvas.drawText(client!!.address, MARGIN, y, textSecondaryPaint)
            y += 11f
        }

        y += 10f

        // Table Header
        val tableHeaderY = y
        val tableHeaderHeight = 22f
        val headerBgPaint = Paint().apply {
            color = Color.rgb(240, 244, 248)
        }
        canvas.drawRoundRect(
            RectF(MARGIN, tableHeaderY, rightX, tableHeaderY + tableHeaderHeight),
            4f, 4f, headerBgPaint
        )

        val col1 = MARGIN + 8f
        val col2 = 330f
        val col3 = 410f
        val col4 = rightX - 8f

        val headerTextPaint = Paint().apply {
            color = Color.rgb(70, 80, 95)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerRightPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("ITEM DESCRIPTION", col1, tableHeaderY + 14f, headerTextPaint)
        canvas.drawText("QTY", col2, tableHeaderY + 14f, headerRightPaint)
        canvas.drawText("UNIT PRICE", col3, tableHeaderY + 14f, headerRightPaint)
        canvas.drawText("AMOUNT", col4, tableHeaderY + 14f, headerRightPaint)

        y = tableHeaderY + tableHeaderHeight + 8f

        // Line Items
        val itemRightPaint = Paint(textPrimaryPaint).apply { textAlign = Paint.Align.RIGHT }
        val itemSecondaryRightPaint = Paint(textSecondaryPaint).apply { textAlign = Paint.Align.RIGHT }

        items.forEachIndexed { index, item ->
            val lineTotal = InvoiceCalculator.calculateLineTotal(item.quantity, item.unitPrice, item.discount)

            canvas.drawText(item.description.ifBlank { "Line item ${index + 1}" }, col1, y + 10f, textPrimaryPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), col2, y + 10f, itemRightPaint)
            canvas.drawText(currency.formatAmount(item.unitPrice), col3, y + 10f, itemRightPaint)
            canvas.drawText(currency.formatAmount(lineTotal), col4, y + 10f, itemRightPaint)

            y += 18f
            canvas.drawLine(MARGIN, y, rightX, y, linePaint)
            y += 6f
        }

        y += 10f

        // Totals Box on Right
        val totalsLeft = 320f
        val labelPaint = Paint(textSecondaryPaint).apply { textAlign = Paint.Align.LEFT }
        val valuePaint = Paint(textPrimaryPaint).apply {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        fun drawTotalRow(label: String, amountStr: String, isBold: Boolean = false) {
            val p = if (isBold) valuePaint else Paint(textPrimaryPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(label, totalsLeft, y, labelPaint)
            canvas.drawText(amountStr, rightX, y, p)
            y += 15f
        }

        drawTotalRow("Subtotal", currency.formatAmount(calc.subtotal))

        if (calc.discountAmount > java.math.BigDecimal.ZERO) {
            val discLabel = if (invoice.discountType == DiscountType.PERCENTAGE) {
                "Discount (${invoice.discountValue}%)"
            } else {
                "Discount"
            }
            drawTotalRow(discLabel, "-${currency.formatAmount(calc.discountAmount)}")
        }

        if (calc.taxAmount > java.math.BigDecimal.ZERO) {
            val taxLabel = if (invoice.taxType == TaxType.PERCENTAGE) {
                "${businessProfile?.taxLabel?.ifBlank { "Tax" } ?: "Tax"} (${invoice.taxRateOrAmount}%)"
            } else {
                businessProfile?.taxLabel?.ifBlank { "Tax" } ?: "Tax"
            }
            drawTotalRow(taxLabel, "+${currency.formatAmount(calc.taxAmount)}")
        }

        if (invoice.shipping > 0) {
            drawTotalRow("Shipping", "+${currency.formatAmount(invoice.shipping)}")
        }

        y += 4f
        canvas.drawLine(totalsLeft, y, rightX, y, linePaint)
        y += 14f

        // Grand Total highlighted
        val grandTotalPaint = Paint(titlePaint).apply {
            textSize = 14f
            color = Color.rgb(24, 76, 120)
            textAlign = Paint.Align.RIGHT
        }
        val grandLabelPaint = Paint(sectionTitlePaint).apply { textSize = 12f }
        canvas.drawText("Total Due", totalsLeft, y, grandLabelPaint)
        canvas.drawText(currency.formatAmount(calc.grandTotal), rightX, y, grandTotalPaint)
        y += 16f

        if (calc.amountPaid > java.math.BigDecimal.ZERO) {
            drawTotalRow("Amount Paid", currency.formatAmount(calc.amountPaid))
            val balPaint = Paint(grandTotalPaint).apply { color = Color.rgb(198, 40, 40) }
            canvas.drawText("Balance Due", totalsLeft, y, grandLabelPaint)
            canvas.drawText(currency.formatAmount(calc.balanceDue), rightX, y, balPaint)
            y += 18f
        }

        // Notes and Payment Terms
        y = maxOf(y + 10f, 680f)
        if (invoice.notes.isNotBlank() || !businessProfile?.invoiceNotes.isNullOrBlank()) {
            val note = invoice.notes.ifBlank { businessProfile?.invoiceNotes.orEmpty() }
            canvas.drawText("NOTES & TERMS", MARGIN, y, sectionTitlePaint)
            y += 12f
            canvas.drawText(note, MARGIN, y, textSecondaryPaint)
            y += 15f
        }

        if (invoice.paymentTerms.isNotBlank() || !businessProfile?.paymentTerms.isNullOrBlank()) {
            val terms = invoice.paymentTerms.ifBlank { businessProfile?.paymentTerms.orEmpty() }
            canvas.drawText("Payment Terms: $terms", MARGIN, y, textSecondaryPaint)
            y += 15f
        }

        // Footer
        val footerPaint = Paint(textSecondaryPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 8f
        }
        canvas.drawText("Generated offline by InvoiceForge — Simple & Reliable Invoicing", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
    }

    fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${file.nameWithoutExtension}")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
}
