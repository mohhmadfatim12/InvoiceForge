package com.invoiceforge.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.invoiceforge.app.domain.calculator.InvoiceCalculator
import com.invoiceforge.app.domain.model.InvoiceCalculationResult
import java.math.BigDecimal

data class InvoiceWithDetails(
    @Embedded
    val invoice: InvoiceEntity,

    @Relation(
        parentColumn = "clientId",
        entityColumn = "id"
    )
    val client: ClientEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val payments: List<PaymentEntity> = emptyList()
) {
    fun calculate(currentTime: Long = System.currentTimeMillis()): InvoiceCalculationResult {
        val lineTotals = items.map {
            InvoiceCalculator.calculateLineTotal(it.quantity, it.unitPrice, it.discount)
        }
        val subtotal = InvoiceCalculator.calculateSubtotal(lineTotals)
        val discountAmount = InvoiceCalculator.calculateDiscountAmount(
            subtotal = subtotal,
            discountType = invoice.discountType,
            discountValue = invoice.discountValue
        )
        val taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO)
        val taxAmount = InvoiceCalculator.calculateTaxAmount(
            taxableAmount = taxableAmount,
            taxType = invoice.taxType,
            taxRateOrAmount = invoice.taxRateOrAmount
        )
        val grandTotal = InvoiceCalculator.calculateGrandTotal(
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            shipping = invoice.shipping
        )
        val amountPaid = InvoiceCalculator.calculateAmountPaid(payments.map { it.amount })
        val balanceDue = InvoiceCalculator.calculateBalanceDue(grandTotal, amountPaid)
        val isOverdue = InvoiceCalculator.isOverdue(invoice.dueDate, amountPaid, grandTotal, currentTime)
        val computedStatus = InvoiceCalculator.determineStatus(
            currentStatus = invoice.status,
            dueDate = invoice.dueDate,
            amountPaid = amountPaid,
            grandTotal = grandTotal,
            currentTime = currentTime
        )

        return InvoiceCalculationResult(
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            amountPaid = amountPaid,
            balanceDue = balanceDue,
            status = computedStatus,
            isOverdue = isOverdue
        )
    }
}
