package com.invoiceforge.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.TaxType

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["clientId"]),
        Index(value = ["status"]),
        Index(value = ["issueDate"]),
        Index(value = ["dueDate"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val invoiceNumber: String,
    val clientId: Long,
    val issueDate: Long,
    val dueDate: Long,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val notes: String = "",
    val paymentTerms: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val taxType: TaxType = TaxType.PERCENTAGE,
    val taxRateOrAmount: Double = 0.0,
    val shipping: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
