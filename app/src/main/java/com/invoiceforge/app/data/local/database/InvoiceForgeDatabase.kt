package com.invoiceforge.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.invoiceforge.app.data.local.dao.BusinessProfileDao
import com.invoiceforge.app.data.local.dao.ClientDao
import com.invoiceforge.app.data.local.dao.InvoiceDao
import com.invoiceforge.app.data.local.dao.InvoiceItemDao
import com.invoiceforge.app.data.local.dao.PaymentDao
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InvoiceForgeDatabase : RoomDatabase() {

    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceForgeDatabase? = null

        fun getInstance(context: Context): InvoiceForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceForgeDatabase::class.java,
                    "invoice_forge.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default business profile
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).businessProfileDao().insertOrUpdate(
                                    BusinessProfileEntity(
                                        id = 1L,
                                        name = "My Freelance Studio",
                                        address = "123 Creative Street, Tech City",
                                        phone = "+1 555-0199",
                                        email = "hello@myfreelancestudio.com",
                                        currency = "USD",
                                        taxLabel = "Tax",
                                        defaultTaxRate = 0.0,
                                        invoiceNotes = "Thank you for your business! Payment is appreciated within 14 days.",
                                        paymentTerms = "Net 14"
                                    )
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
