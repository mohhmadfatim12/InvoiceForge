package com.invoiceforge.app.data.local.database

import androidx.room.TypeConverter
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.PaymentMethod
import com.invoiceforge.app.domain.model.TaxType

class Converters {
    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus?): String? = status?.name

    @TypeConverter
    fun toInvoiceStatus(value: String?): InvoiceStatus = InvoiceStatus.fromString(value)

    @TypeConverter
    fun fromDiscountType(type: DiscountType?): String? = type?.name

    @TypeConverter
    fun toDiscountType(value: String?): DiscountType = DiscountType.fromString(value)

    @TypeConverter
    fun fromTaxType(type: TaxType?): String? = type?.name

    @TypeConverter
    fun toTaxType(value: String?): TaxType = TaxType.fromString(value)

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? = method?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod = PaymentMethod.fromString(value)
}
