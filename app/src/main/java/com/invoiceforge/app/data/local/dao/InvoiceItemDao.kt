package com.invoiceforge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY orderIndex ASC, id ASC")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY orderIndex ASC, id ASC")
    suspend fun getItemsForInvoiceSync(invoiceId: Long): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InvoiceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>): List<Long>

    @Update
    suspend fun updateItem(item: InvoiceItemEntity): Int

    @Delete
    suspend fun deleteItem(item: InvoiceItemEntity): Int

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long): Int

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAll()
}
