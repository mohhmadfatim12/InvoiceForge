package com.invoiceforge.app.di

import android.content.Context
import com.invoiceforge.app.data.backup.BackupManager
import com.invoiceforge.app.data.local.database.InvoiceForgeDatabase
import com.invoiceforge.app.data.local.preferences.UserPreferencesManager
import com.invoiceforge.app.data.pdf.PdfInvoiceGenerator
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.ClientRepository
import com.invoiceforge.app.data.repository.InvoiceRepository

interface AppContainer {
    val database: InvoiceForgeDatabase
    val businessProfileRepository: BusinessProfileRepository
    val clientRepository: ClientRepository
    val invoiceRepository: InvoiceRepository
    val pdfInvoiceGenerator: PdfInvoiceGenerator
    val backupManager: BackupManager
    val userPreferencesManager: UserPreferencesManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: InvoiceForgeDatabase by lazy {
        InvoiceForgeDatabase.getInstance(context)
    }

    override val businessProfileRepository: BusinessProfileRepository by lazy {
        BusinessProfileRepository(database.businessProfileDao())
    }

    override val clientRepository: ClientRepository by lazy {
        ClientRepository(database.clientDao(), database.invoiceDao())
    }

    override val invoiceRepository: InvoiceRepository by lazy {
        InvoiceRepository(database)
    }

    override val pdfInvoiceGenerator: PdfInvoiceGenerator by lazy {
        PdfInvoiceGenerator(context)
    }

    override val backupManager: BackupManager by lazy {
        BackupManager(database)
    }

    override val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(context)
    }
}
