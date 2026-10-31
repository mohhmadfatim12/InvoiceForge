package com.invoiceforge.app.data.repository

import androidx.room.withTransaction
import com.invoiceforge.app.data.local.database.InvoiceForgeDatabase
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.local.entity.PaymentEntity
import com.invoiceforge.app.domain.calculator.InvoiceCalculator
import com.invoiceforge.app.domain.model.DashboardStats
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.MonthlyRevenue
import com.invoiceforge.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvoiceRepository(
    private val database: InvoiceForgeDatabase
) {
    private val invoiceDao = database.invoiceDao()
    private val itemDao = database.invoiceItemDao()
    private val paymentDao = database.paymentDao()

    val allInvoices: Flow<List<InvoiceWithDetails>> = invoiceDao.getAllInvoicesWithDetails()
        .map { list ->
            // Check and update any overdue invoices dynamically
            val now = System.currentTimeMillis()
            list.map { item ->
                val calc = item.calculate(now)
                if (item.invoice.status != InvoiceStatus.DRAFT &&
                    item.invoice.status != InvoiceStatus.PAID &&
                    item.invoice.status != calc.status
                ) {
                    // Update in database in background if status drifted
                    invoiceDao.updateInvoiceStatus(item.invoice.id, calc.status, now)
                    item.copy(invoice = item.invoice.copy(status = calc.status))
                } else {
                    item
                }
            }
        }

    fun getInvoiceById(id: Long): Flow<InvoiceWithDetails?> = invoiceDao.getInvoiceWithDetailsById(id)

    suspend fun getInvoiceByIdSync(id: Long): InvoiceWithDetails? = invoiceDao.getInvoiceWithDetailsByIdSync(id)

    fun getInvoicesForClient(clientId: Long): Flow<List<InvoiceWithDetails>> =
        invoiceDao.getInvoicesForClient(clientId)

    suspend fun generateNextInvoiceNumber(prefix: String = "INV-"): String {
        val lastNumber = invoiceDao.getLastInvoiceNumber()
        return InvoiceCalculator.generateNextInvoiceNumber(lastNumber, prefix)
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        var invoiceId = invoice.id
        database.withTransaction {
            if (invoiceId == 0L) {
                invoiceId = invoiceDao.insertInvoice(invoice.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            } else {
                invoiceDao.updateInvoice(invoice.copy(updatedAt = System.currentTimeMillis()))
                itemDao.deleteItemsForInvoice(invoiceId)
            }

            val indexedItems = items.mapIndexed { index, item ->
                item.copy(id = 0L, invoiceId = invoiceId, orderIndex = index)
            }
            itemDao.insertItems(indexedItems)

            // Re-evaluate invoice status
            recomputeAndPersistStatus(invoiceId)
        }
        return invoiceId
    }

    suspend fun duplicateInvoice(sourceInvoiceId: Long, newInvoiceNumber: String): Long {
        val source = invoiceDao.getInvoiceWithDetailsByIdSync(sourceInvoiceId)
            ?: throw IllegalArgumentException("Source invoice not found")

        val now = System.currentTimeMillis()
        val dueDelta = source.invoice.dueDate - source.invoice.issueDate
        val newDueDate = if (dueDelta > 0) now + dueDelta else now + (14L * 24 * 60 * 60 * 1000)

        val newInvoice = source.invoice.copy(
            id = 0L,
            invoiceNumber = newInvoiceNumber,
            issueDate = now,
            dueDate = newDueDate,
            status = InvoiceStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )

        return database.withTransaction {
            val newId = invoiceDao.insertInvoice(newInvoice)
            val newItems = source.items.mapIndexed { index, item ->
                item.copy(id = 0L, invoiceId = newId, orderIndex = index)
            }
            itemDao.insertItems(newItems)
            newId
        }
    }

    suspend fun deleteInvoice(invoiceId: Long): Boolean {
        return invoiceDao.deleteInvoiceById(invoiceId) > 0
    }

    suspend fun recordPayment(
        invoiceId: Long,
        amount: Double,
        method: PaymentMethod,
        note: String,
        paymentDate: Long = System.currentTimeMillis()
    ): Result<Long> {
        val invoiceWithDetails = invoiceDao.getInvoiceWithDetailsByIdSync(invoiceId)
            ?: return Result.failure(IllegalArgumentException("Invoice not found"))

        val calc = invoiceWithDetails.calculate()
        val proposedAmount = BigDecimal.valueOf(amount)

        if (proposedAmount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero"))
        }

        if (proposedAmount > calc.balanceDue) {
            return Result.failure(
                IllegalArgumentException("Payment amount (${calc.balanceDue}) cannot exceed balance due")
            )
        }

        return database.withTransaction {
            val paymentId = paymentDao.insertPayment(
                PaymentEntity(
                    id = 0L,
                    invoiceId = invoiceId,
                    amount = amount,
                    paymentDate = paymentDate,
                    paymentMethod = method,
                    note = note
                )
            )
            recomputeAndPersistStatus(invoiceId)
            Result.success(paymentId)
        }
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        database.withTransaction {
            paymentDao.deletePayment(payment)
            recomputeAndPersistStatus(payment.invoiceId)
        }
    }

    private suspend fun recomputeAndPersistStatus(invoiceId: Long) {
        val details = invoiceDao.getInvoiceWithDetailsByIdSync(invoiceId) ?: return
        val calc = details.calculate()
        invoiceDao.updateInvoiceStatus(invoiceId, calc.status, System.currentTimeMillis())
    }

    fun getDashboardStats(): Flow<DashboardStats> {
        return invoiceDao.getAllInvoicesWithDetails().map { list ->
            val now = System.currentTimeMillis()
            var totalInvoiced = BigDecimal.ZERO
            var totalReceived = BigDecimal.ZERO
            var totalOutstanding = BigDecimal.ZERO
            var overdueCount = 0
            var thisMonthCount = 0
            var draftCount = 0
            var pendingCount = 0
            var partiallyPaidCount = 0
            var paidCount = 0

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Setup last 6 months buckets
            val monthMap = LinkedHashMap<String, Pair<BigDecimal, BigDecimal>>()
            val monthFormat = SimpleDateFormat("MMM", Locale.US)
            val tempCal = Calendar.getInstance()
            for (i in 5 downTo 0) {
                tempCal.timeInMillis = now
                tempCal.add(Calendar.MONTH, -i)
                val label = monthFormat.format(tempCal.time)
                monthMap[label] = Pair(BigDecimal.ZERO, BigDecimal.ZERO)
            }

            list.forEach { item ->
                val calc = item.calculate(now)
                totalInvoiced = totalInvoiced.add(calc.grandTotal)
                totalReceived = totalReceived.add(calc.amountPaid)
                totalOutstanding = totalOutstanding.add(calc.balanceDue)

                if (calc.isOverdue) overdueCount++

                calendar.timeInMillis = item.invoice.issueDate
                if (calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear) {
                    thisMonthCount++
                }

                when (calc.status) {
                    InvoiceStatus.DRAFT -> draftCount++
                    InvoiceStatus.PENDING -> pendingCount++
                    InvoiceStatus.PARTIALLY_PAID -> partiallyPaidCount++
                    InvoiceStatus.PAID -> paidCount++
                    InvoiceStatus.OVERDUE -> overdueCount++
                }

                // Month bucket
                val invMonthLabel = monthFormat.format(item.invoice.issueDate)
                if (monthMap.containsKey(invMonthLabel)) {
                    val current = monthMap[invMonthLabel] ?: Pair(BigDecimal.ZERO, BigDecimal.ZERO)
                    monthMap[invMonthLabel] = Pair(
                        current.first.add(calc.grandTotal),
                        current.second.add(calc.amountPaid)
                    )
                }
            }

            val monthlyTrends = monthMap.map { (month, pair) ->
                MonthlyRevenue(
                    monthLabel = month,
                    invoicedAmount = pair.first,
                    receivedAmount = pair.second
                )
            }

            DashboardStats(
                totalInvoiced = totalInvoiced,
                totalReceived = totalReceived,
                totalOutstanding = totalOutstanding,
                overdueCount = overdueCount,
                invoicesThisMonthCount = thisMonthCount,
                draftCount = draftCount,
                pendingCount = pendingCount,
                partiallyPaidCount = partiallyPaidCount,
                paidCount = paidCount,
                monthlyTrends = monthlyTrends
            )
        }
    }
}
