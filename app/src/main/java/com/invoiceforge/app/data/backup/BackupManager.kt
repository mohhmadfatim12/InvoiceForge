package com.invoiceforge.app.data.backup

import androidx.room.withTransaction
import com.invoiceforge.app.data.local.database.InvoiceForgeDatabase
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.local.entity.PaymentEntity
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.PaymentMethod
import com.invoiceforge.app.domain.model.TaxType
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val clientsCount: Int,
    val invoicesCount: Int,
    val itemsCount: Int,
    val paymentsCount: Int
)

class BackupManager(private val database: InvoiceForgeDatabase) {

    companion object {
        const val BACKUP_VERSION = 1
        const val APP_ID = "InvoiceForge"
    }

    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())

        // Business Profile
        val profile = database.businessProfileDao().getProfileSync()
        if (profile != null) {
            val pObj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("address", profile.address)
                put("phone", profile.phone)
                put("email", profile.email)
                put("currency", profile.currency)
                put("taxLabel", profile.taxLabel)
                put("defaultTaxRate", profile.defaultTaxRate)
                put("invoiceNotes", profile.invoiceNotes)
                put("paymentTerms", profile.paymentTerms)
            }
            root.put("businessProfile", pObj)
        }

        // Clients
        val clients = database.clientDao().getAllClientsSync()
        val clientsArr = JSONArray()
        clients.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("company", c.company)
                put("phone", c.phone)
                put("email", c.email)
                put("address", c.address)
                put("notes", c.notes)
                put("createdAt", c.createdAt)
            }
            clientsArr.put(obj)
        }
        root.put("clients", clientsArr)

        // Invoices with items and payments
        val invoices = database.invoiceDao().getAllInvoicesWithDetailsSync()
        val invoicesArr = JSONArray()
        val itemsArr = JSONArray()
        val paymentsArr = JSONArray()

        invoices.forEach { item ->
            val inv = item.invoice
            val invObj = JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("clientId", inv.clientId)
                put("issueDate", inv.issueDate)
                put("dueDate", inv.dueDate)
                put("status", inv.status.name)
                put("notes", inv.notes)
                put("paymentTerms", inv.paymentTerms)
                put("discountType", inv.discountType.name)
                put("discountValue", inv.discountValue)
                put("taxType", inv.taxType.name)
                put("taxRateOrAmount", inv.taxRateOrAmount)
                put("shipping", inv.shipping)
                put("createdAt", inv.createdAt)
                put("updatedAt", inv.updatedAt)
            }
            invoicesArr.put(invObj)

            item.items.forEach { line ->
                val lineObj = JSONObject().apply {
                    put("id", line.id)
                    put("invoiceId", line.invoiceId)
                    put("description", line.description)
                    put("quantity", line.quantity)
                    put("unitPrice", line.unitPrice)
                    put("discount", line.discount)
                    put("orderIndex", line.orderIndex)
                }
                itemsArr.put(lineObj)
            }

            item.payments.forEach { pay ->
                val payObj = JSONObject().apply {
                    put("id", pay.id)
                    put("invoiceId", pay.invoiceId)
                    put("amount", pay.amount)
                    put("paymentDate", pay.paymentDate)
                    put("paymentMethod", pay.paymentMethod.name)
                    put("note", pay.note)
                }
                paymentsArr.put(payObj)
            }
        }

        root.put("invoices", invoicesArr)
        root.put("invoiceItems", itemsArr)
        root.put("payments", paymentsArr)

        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<BackupRestoreSummary> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != APP_ID) {
                return Result.failure(IllegalArgumentException("Invalid backup file: Not an InvoiceForge backup"))
            }

            val version = root.optInt("version", -1)
            if (version != BACKUP_VERSION) {
                return Result.failure(IllegalArgumentException("Unsupported backup version: $version (expected $BACKUP_VERSION)"))
            }

            // Parse Business Profile
            val profile = if (root.has("businessProfile")) {
                val p = root.getJSONObject("businessProfile")
                BusinessProfileEntity(
                    id = 1L,
                    name = p.optString("name", "My Business"),
                    address = p.optString("address", ""),
                    phone = p.optString("phone", ""),
                    email = p.optString("email", ""),
                    currency = p.optString("currency", "USD"),
                    taxLabel = p.optString("taxLabel", "Tax"),
                    defaultTaxRate = p.optDouble("defaultTaxRate", 0.0),
                    invoiceNotes = p.optString("invoiceNotes", ""),
                    paymentTerms = p.optString("paymentTerms", "")
                )
            } else null

            // Parse Clients
            val clientsList = mutableListOf<ClientEntity>()
            if (root.has("clients")) {
                val arr = root.getJSONArray("clients")
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    clientsList.add(
                        ClientEntity(
                            id = c.getLong("id"),
                            name = c.getString("name"),
                            company = c.optString("company", ""),
                            phone = c.optString("phone", ""),
                            email = c.optString("email", ""),
                            address = c.optString("address", ""),
                            notes = c.optString("notes", ""),
                            createdAt = c.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Invoices
            val invoicesList = mutableListOf<InvoiceEntity>()
            if (root.has("invoices")) {
                val arr = root.getJSONArray("invoices")
                for (i in 0 until arr.length()) {
                    val inv = arr.getJSONObject(i)
                    invoicesList.add(
                        InvoiceEntity(
                            id = inv.getLong("id"),
                            invoiceNumber = inv.getString("invoiceNumber"),
                            clientId = inv.getLong("clientId"),
                            issueDate = inv.getLong("issueDate"),
                            dueDate = inv.getLong("dueDate"),
                            status = InvoiceStatus.fromString(inv.optString("status", "PENDING")),
                            notes = inv.optString("notes", ""),
                            paymentTerms = inv.optString("paymentTerms", ""),
                            discountType = DiscountType.fromString(inv.optString("discountType", "PERCENTAGE")),
                            discountValue = inv.optDouble("discountValue", 0.0),
                            taxType = TaxType.fromString(inv.optString("taxType", "PERCENTAGE")),
                            taxRateOrAmount = inv.optDouble("taxRateOrAmount", 0.0),
                            shipping = inv.optDouble("shipping", 0.0),
                            createdAt = inv.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = inv.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Items
            val itemsList = mutableListOf<InvoiceItemEntity>()
            if (root.has("invoiceItems")) {
                val arr = root.getJSONArray("invoiceItems")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    itemsList.add(
                        InvoiceItemEntity(
                            id = item.getLong("id"),
                            invoiceId = item.getLong("invoiceId"),
                            description = item.getString("description"),
                            quantity = item.optDouble("quantity", 1.0),
                            unitPrice = item.optDouble("unitPrice", 0.0),
                            discount = item.optDouble("discount", 0.0),
                            orderIndex = item.optInt("orderIndex", i)
                        )
                    )
                }
            }

            // Parse Payments
            val paymentsList = mutableListOf<PaymentEntity>()
            if (root.has("payments")) {
                val arr = root.getJSONArray("payments")
                for (i in 0 until arr.length()) {
                    val pay = arr.getJSONObject(i)
                    paymentsList.add(
                        PaymentEntity(
                            id = pay.getLong("id"),
                            invoiceId = pay.getLong("invoiceId"),
                            amount = pay.getDouble("amount"),
                            paymentDate = pay.optLong("paymentDate", System.currentTimeMillis()),
                            paymentMethod = PaymentMethod.fromString(pay.optString("paymentMethod", "CASH")),
                            note = pay.optString("note", "")
                        )
                    )
                }
            }

            // Restore inside transaction
            database.withTransaction {
                // Clear existing
                database.paymentDao().deleteAll()
                database.invoiceItemDao().deleteAll()
                database.invoiceDao().deleteAll()
                database.clientDao().deleteAll()

                if (profile != null) {
                    database.businessProfileDao().insertOrUpdate(profile)
                }
                clientsList.forEach { database.clientDao().insertClient(it) }
                invoicesList.forEach { database.invoiceDao().insertInvoice(it) }
                database.invoiceItemDao().insertItems(itemsList)
                database.paymentDao().insertPayments(paymentsList)
            }

            Result.success(
                BackupRestoreSummary(
                    clientsCount = clientsList.size,
                    invoicesCount = invoicesList.size,
                    itemsCount = itemsList.size,
                    paymentsCount = paymentsList.size
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to restore backup: ${e.message}", e))
        }
    }
}
