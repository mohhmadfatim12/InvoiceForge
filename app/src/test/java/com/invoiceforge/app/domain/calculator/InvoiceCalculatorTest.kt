package com.invoiceforge.app.domain.calculator

import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.TaxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class InvoiceCalculatorTest {

    // 1. Line-item total calculation
    @Test
    fun testLineItemTotal_standard() {
        val total = InvoiceCalculator.calculateLineTotal(quantity = 5.0, unitPrice = 20.0, lineDiscount = 10.0)
        // 5 * 20 - 10 = 90.00
        assertEquals(BigDecimal("90.00"), total)
    }

    @Test
    fun testLineItemTotal_noDiscount() {
        val total = InvoiceCalculator.calculateLineTotal(quantity = 2.5, unitPrice = 40.0)
        // 2.5 * 40 = 100.00
        assertEquals(BigDecimal("100.00"), total)
    }

    @Test
    fun testLineItemTotal_preventNegative() {
        val total = InvoiceCalculator.calculateLineTotal(quantity = 1.0, unitPrice = 10.0, lineDiscount = 50.0)
        // cannot be negative
        assertEquals(BigDecimal("0.00"), total)
    }

    // 2. Invoice subtotal calculation
    @Test
    fun testInvoiceSubtotal() {
        val lines = listOf(
            BigDecimal("100.00"),
            BigDecimal("250.50"),
            BigDecimal("49.50")
        )
        val subtotal = InvoiceCalculator.calculateSubtotal(lines)
        assertEquals(BigDecimal("400.00"), subtotal)
    }

    // 3. Percentage discount calculation
    @Test
    fun testPercentageDiscount() {
        val subtotal = BigDecimal("200.00")
        val discount = InvoiceCalculator.calculateDiscountAmount(
            subtotal = subtotal,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 15.0
        )
        // 200 * 15% = 30.00
        assertEquals(BigDecimal("30.00"), discount)
    }

    // 4. Fixed discount calculation
    @Test
    fun testFixedDiscount() {
        val subtotal = BigDecimal("200.00")
        val discount = InvoiceCalculator.calculateDiscountAmount(
            subtotal = subtotal,
            discountType = DiscountType.FIXED,
            discountValue = 45.00
        )
        assertEquals(BigDecimal("45.00"), discount)
    }

    @Test
    fun testDiscount_cannotExceedSubtotal() {
        val subtotal = BigDecimal("50.00")
        val discount = InvoiceCalculator.calculateDiscountAmount(
            subtotal = subtotal,
            discountType = DiscountType.FIXED,
            discountValue = 100.00
        )
        // Capped at subtotal 50.00
        assertEquals(BigDecimal("50.00"), discount)
    }

    // 5. Percentage tax calculation
    @Test
    fun testPercentageTax() {
        val taxable = BigDecimal("100.00")
        val tax = InvoiceCalculator.calculateTaxAmount(
            taxableAmount = taxable,
            taxType = TaxType.PERCENTAGE,
            taxRateOrAmount = 8.5
        )
        // 100 * 8.5% = 8.50
        assertEquals(BigDecimal("8.50"), tax)
    }

    // 6. Fixed tax calculation
    @Test
    fun testFixedTax() {
        val taxable = BigDecimal("100.00")
        val tax = InvoiceCalculator.calculateTaxAmount(
            taxableAmount = taxable,
            taxType = TaxType.FIXED,
            taxRateOrAmount = 15.00
        )
        assertEquals(BigDecimal("15.00"), tax)
    }

    // 7. Grand total calculation
    @Test
    fun testGrandTotal_allComponents() {
        val subtotal = BigDecimal("1000.00")
        val discount = BigDecimal("100.00") // 900 net
        val tax = BigDecimal("90.00") // 10% on 900 = 90
        val shipping = 25.00

        val grandTotal = InvoiceCalculator.calculateGrandTotal(
            subtotal = subtotal,
            discountAmount = discount,
            taxAmount = tax,
            shipping = shipping
        )
        // 1000 - 100 + 90 + 25 = 1015.00
        assertEquals(BigDecimal("1015.00"), grandTotal)
    }

    // 8. Payment allocation and balance due calculation
    @Test
    fun testBalanceDue_and_amountPaid() {
        val payments = listOf(100.0, 150.50, 49.50)
        val amountPaid = InvoiceCalculator.calculateAmountPaid(payments)
        assertEquals(BigDecimal("300.00"), amountPaid)

        val grandTotal = BigDecimal("500.00")
        val balanceDue = InvoiceCalculator.calculateBalanceDue(grandTotal, amountPaid)
        assertEquals(BigDecimal("200.00"), balanceDue)
    }

    @Test
    fun testBalanceDue_cannotBeNegative() {
        val grandTotal = BigDecimal("200.00")
        val amountPaid = BigDecimal("250.00")
        val balanceDue = InvoiceCalculator.calculateBalanceDue(grandTotal, amountPaid)
        assertEquals(BigDecimal("0.00"), balanceDue)
    }

    // 9. Invoice status transitions
    @Test
    fun testStatusTransitions() {
        val now = 1000000L
        val futureDue = 2000000L
        val pastDue = 500000L
        val total = BigDecimal("500.00")

        // Draft remains Draft
        val draftStatus = InvoiceCalculator.determineStatus(
            currentStatus = InvoiceStatus.DRAFT,
            dueDate = futureDue,
            amountPaid = BigDecimal.ZERO,
            grandTotal = total,
            currentTime = now
        )
        assertEquals(InvoiceStatus.DRAFT, draftStatus)

        // No payment, due in future -> PENDING
        val pendingStatus = InvoiceCalculator.determineStatus(
            currentStatus = InvoiceStatus.PENDING,
            dueDate = futureDue,
            amountPaid = BigDecimal.ZERO,
            grandTotal = total,
            currentTime = now
        )
        assertEquals(InvoiceStatus.PENDING, pendingStatus)

        // Partial payment, due in future -> PARTIALLY_PAID
        val partialStatus = InvoiceCalculator.determineStatus(
            currentStatus = InvoiceStatus.PENDING,
            dueDate = futureDue,
            amountPaid = BigDecimal("200.00"),
            grandTotal = total,
            currentTime = now
        )
        assertEquals(InvoiceStatus.PARTIALLY_PAID, partialStatus)

        // Full payment -> PAID
        val paidStatus = InvoiceCalculator.determineStatus(
            currentStatus = InvoiceStatus.PENDING,
            dueDate = futureDue,
            amountPaid = BigDecimal("500.00"),
            grandTotal = total,
            currentTime = now
        )
        assertEquals(InvoiceStatus.PAID, paidStatus)

        // Unpaid and due date passed -> OVERDUE
        val overdueStatus = InvoiceCalculator.determineStatus(
            currentStatus = InvoiceStatus.PENDING,
            dueDate = pastDue,
            amountPaid = BigDecimal.ZERO,
            grandTotal = total,
            currentTime = now
        )
        assertEquals(InvoiceStatus.OVERDUE, overdueStatus)
    }

    // 10. Invoice number generation
    @Test
    fun testInvoiceNumberGeneration() {
        // null or blank -> INV-0001
        assertEquals("INV-0001", InvoiceCalculator.generateNextInvoiceNumber(null))
        assertEquals("INV-0001", InvoiceCalculator.generateNextInvoiceNumber(""))

        // INV-0001 -> INV-0002
        assertEquals("INV-0002", InvoiceCalculator.generateNextInvoiceNumber("INV-0001"))

        // INV-0099 -> INV-0100
        assertEquals("INV-0100", InvoiceCalculator.generateNextInvoiceNumber("INV-0099"))

        // INV-0999 -> INV-1000
        assertEquals("INV-1000", InvoiceCalculator.generateNextInvoiceNumber("INV-0999"))
    }

    // 11. Overdue detection
    @Test
    fun testOverdueDetection() {
        val now = 1000000L
        val past = 500000L
        val future = 2000000L
        val total = BigDecimal("100.00")

        // Unpaid, past due -> True
        assertTrue(InvoiceCalculator.isOverdue(past, BigDecimal.ZERO, total, now))

        // Unpaid, future due -> False
        assertFalse(InvoiceCalculator.isOverdue(future, BigDecimal.ZERO, total, now))

        // Fully paid, past due -> False (not overdue if paid)
        assertFalse(InvoiceCalculator.isOverdue(past, total, total, now))
    }
}
