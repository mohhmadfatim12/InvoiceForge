package com.invoiceforge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    val name: String = "My Business",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val currency: String = "USD",
    val taxLabel: String = "Tax",
    val defaultTaxRate: Double = 0.0,
    val invoiceNotes: String = "Thank you for your business!",
    val paymentTerms: String = "Due upon receipt"
)
