package com.invoiceforge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.invoiceforge.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC, id DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC, id DESC")
    suspend fun getPaymentsForInvoiceSync(invoiceId: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>): List<Long>

    @Update
    suspend fun updatePayment(payment: PaymentEntity): Int

    @Delete
    suspend fun deletePayment(payment: PaymentEntity): Int

    @Query("DELETE FROM payments WHERE invoiceId = :invoiceId")
    suspend fun deletePaymentsForInvoice(invoiceId: Long): Int

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
