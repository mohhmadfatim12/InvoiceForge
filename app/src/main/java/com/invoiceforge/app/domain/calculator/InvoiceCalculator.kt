package com.invoiceforge.app.domain.calculator

import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.TaxType
import java.math.BigDecimal
import java.math.RoundingMode

object InvoiceCalculator {

    private val SCALE = 2
    private val ROUNDING = RoundingMode.HALF_UP
    private val ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING)
    private val HUNDRED = BigDecimal("100.00")

    fun toBigDecimal(value: Double): BigDecimal {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING)
    }

    /**
     * line_total = quantity * unit_price - line_discount
     * Prevent negative line totals.
     */
    fun calculateLineTotal(quantity: Double, unitPrice: Double, lineDiscount: Double = 0.0): BigDecimal {
        val qty = BigDecimal.valueOf(maxOf(0.0, quantity))
        val price = BigDecimal.valueOf(maxOf(0.0, unitPrice))
        val discount = BigDecimal.valueOf(maxOf(0.0, lineDiscount))

        val gross = qty.multiply(price).setScale(SCALE, ROUNDING)
        val net = gross.subtract(discount).setScale(SCALE, ROUNDING)
        return net.max(ZERO)
    }

    /**
     * invoice_subtotal = sum(line_total)
     */
    fun calculateSubtotal(lineTotals: List<BigDecimal>): BigDecimal {
        return lineTotals.fold(ZERO) { acc, total -> acc.add(total) }.setScale(SCALE, ROUNDING)
    }

    /**
     * Calculate invoice discount amount.
     * Capped at subtotal so discount cannot exceed subtotal.
     */
    fun calculateDiscountAmount(
        subtotal: BigDecimal,
        discountType: DiscountType,
        discountValue: Double
    ): BigDecimal {
        val cleanValue = BigDecimal.valueOf(maxOf(0.0, discountValue)).setScale(SCALE, ROUNDING)
        if (subtotal <= ZERO || cleanValue <= ZERO) return ZERO

        val calculated = when (discountType) {
            DiscountType.PERCENTAGE -> {
                val clampedPercent = cleanValue.min(HUNDRED)
                subtotal.multiply(clampedPercent).divide(HUNDRED, SCALE, ROUNDING)
            }
            DiscountType.FIXED -> cleanValue
        }
        return calculated.min(subtotal).max(ZERO)
    }

    /**
     * Calculate tax amount based on taxable amount (subtotal - discount).
     */
    fun calculateTaxAmount(
        taxableAmount: BigDecimal,
        taxType: TaxType,
        taxRateOrAmount: Double
    ): BigDecimal {
        val cleanRate = BigDecimal.valueOf(maxOf(0.0, taxRateOrAmount)).setScale(SCALE, ROUNDING)
        if (taxableAmount <= ZERO || cleanRate <= ZERO) return ZERO

        val calculated = when (taxType) {
            TaxType.PERCENTAGE -> {
                taxableAmount.multiply(cleanRate).divide(HUNDRED, SCALE, ROUNDING)
            }
            TaxType.FIXED -> cleanRate
        }
        return calculated.max(ZERO)
    }

    /**
     * grand_total = invoice_subtotal - discount_amount + tax_amount + shipping
     * Cannot be negative.
     */
    fun calculateGrandTotal(
        subtotal: BigDecimal,
        discountAmount: BigDecimal,
        taxAmount: BigDecimal,
        shipping: Double = 0.0
    ): BigDecimal {
        val ship = BigDecimal.valueOf(maxOf(0.0, shipping)).setScale(SCALE, ROUNDING)
        val total = subtotal.subtract(discountAmount).add(taxAmount).add(ship)
        return total.max(ZERO).setScale(SCALE, ROUNDING)
    }

    /**
     * amount_paid = sum(valid payments)
     */
    fun calculateAmountPaid(paymentAmounts: List<Double>): BigDecimal {
        return paymentAmounts
            .map { BigDecimal.valueOf(maxOf(0.0, it)) }
            .fold(ZERO) { acc, amt -> acc.add(amt) }
            .setScale(SCALE, ROUNDING)
    }

    /**
     * balance_due = grand_total - amount_paid
     * Cannot be negative.
     */
    fun calculateBalanceDue(grandTotal: BigDecimal, amountPaid: BigDecimal): BigDecimal {
        val balance = grandTotal.subtract(amountPaid).setScale(SCALE, ROUNDING)
        return balance.max(ZERO)
    }

    /**
     * Determines whether an invoice is overdue based on due date, payments, and total.
     */
    fun isOverdue(dueDate: Long, amountPaid: BigDecimal, grandTotal: BigDecimal, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (grandTotal <= ZERO) return false
        val balance = calculateBalanceDue(grandTotal, amountPaid)
        return balance > ZERO && currentTime > dueDate
    }

    /**
     * Automatically calculate appropriate status for an invoice.
     */
    fun determineStatus(
        currentStatus: InvoiceStatus,
        dueDate: Long,
        amountPaid: BigDecimal,
        grandTotal: BigDecimal,
        currentTime: Long = System.currentTimeMillis()
    ): InvoiceStatus {
        if (currentStatus == InvoiceStatus.DRAFT) return InvoiceStatus.DRAFT

        val balance = calculateBalanceDue(grandTotal, amountPaid)
        return when {
            balance <= ZERO && grandTotal > ZERO -> InvoiceStatus.PAID
            amountPaid > ZERO && balance > ZERO -> {
                if (currentTime > dueDate) InvoiceStatus.OVERDUE else InvoiceStatus.PARTIALLY_PAID
            }
            else -> {
                if (currentTime > dueDate) InvoiceStatus.OVERDUE else InvoiceStatus.PENDING
            }
        }
    }

    /**
     * Automatically format next invoice number:
     * e.g., INV-0001, INV-0002 ...
     */
    fun generateNextInvoiceNumber(lastInvoiceNumber: String?, prefix: String = "INV-"): String {
        if (lastInvoiceNumber.isNullOrBlank()) {
            return "${prefix}0001"
        }
        val regex = Regex("^${Regex.escape(prefix)}(\\d+)$")
        val match = regex.find(lastInvoiceNumber.trim())
        if (match != null) {
            val numStr = match.groupValues[1]
            val nextVal = (numStr.toLongOrNull() ?: 0L) + 1L
            val digits = maxOf(4, numStr.length)
            return "$prefix${nextVal.toString().padStart(digits, '0')}"
        }

        // Generic fallback if custom prefix or format
        val digitsOnly = lastInvoiceNumber.filter { it.isDigit() }
        val nextVal = (digitsOnly.toLongOrNull() ?: 0L) + 1L
        return "$prefix${nextVal.toString().padStart(4, '0')}"
    }
}
