package com.invoiceforge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.domain.model.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    fun getAllInvoicesWithDetails(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    suspend fun getAllInvoicesWithDetailsSync(): List<InvoiceWithDetails>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceWithDetailsById(id: Long): Flow<InvoiceWithDetails?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithDetailsByIdSync(id: Long): InvoiceWithDetails?

    @Transaction
    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY issueDate DESC")
    fun getInvoicesForClient(clientId: Long): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY issueDate DESC")
    suspend fun getInvoicesForClientSync(clientId: Long): List<InvoiceWithDetails>

    @Query("SELECT invoiceNumber FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun getLastInvoiceNumber(): String?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :number LIMIT 1")
    suspend fun getInvoiceByNumber(number: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity): Int

    @Query("UPDATE invoices SET status = :status, updatedAt = :updatedAt WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity): Int

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long): Int

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()
}
