package com.invoiceforge.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.backup.BackupManager
import com.invoiceforge.app.data.export.CsvExporter
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.preferences.ThemeMode
import com.invoiceforge.app.data.local.preferences.UserPreferencesManager
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.ClientRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.CurrencyConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val businessProfile: BusinessProfileEntity = BusinessProfileEntity(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val invoicePrefix: String = "INV-",
    val isLoading: Boolean = true,
    val message: String? = null,
    val isError: Boolean = false
)

class SettingsViewModel(
    private val profileRepository: BusinessProfileRepository,
    private val preferencesManager: UserPreferencesManager,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _message = MutableStateFlow<Pair<String, Boolean>?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        profileRepository.profile,
        preferencesManager.themeMode,
        preferencesManager.invoicePrefix,
        _message
    ) { profile, theme, prefix, msg ->
        SettingsUiState(
            businessProfile = profile ?: BusinessProfileEntity(),
            themeMode = theme,
            invoicePrefix = prefix,
            isLoading = false,
            message = msg?.first,
            isError = msg?.second ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    fun updateBusinessProfile(
        name: String,
        address: String,
        phone: String,
        email: String,
        currency: String,
        taxLabel: String,
        defaultTaxRate: Double,
        invoiceNotes: String,
        paymentTerms: String
    ) {
        viewModelScope.launch {
            val updated = BusinessProfileEntity(
                id = 1L,
                name = name.trim(),
                address = address.trim(),
                phone = phone.trim(),
                email = email.trim(),
                currency = currency.trim(),
                taxLabel = taxLabel.trim().ifBlank { "Tax" },
                defaultTaxRate = defaultTaxRate,
                invoiceNotes = invoiceNotes.trim(),
                paymentTerms = paymentTerms.trim()
            )
            profileRepository.saveProfile(updated)
            _message.value = Pair("Business profile saved successfully", false)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setInvoicePrefix(prefix: String) {
        viewModelScope.launch {
            preferencesManager.setInvoicePrefix(prefix)
            _message.value = Pair("Invoice prefix updated to $prefix", false)
        }
    }

    fun exportInvoicesCsv(context: Context) {
        viewModelScope.launch {
            try {
                val invoices = invoiceRepository.allInvoices.firstOrNull() ?: emptyList()
                val csvContent = CsvExporter.exportInvoicesToCsv(invoices)
                shareExportFile(context, "invoices_export.csv", "text/csv", csvContent)
                _message.value = Pair("Invoices exported successfully (${invoices.size} records)", false)
            } catch (e: Exception) {
                _message.value = Pair("Failed to export invoices: ${e.message}", true)
            }
        }
    }

    fun exportClientsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val clients = clientRepository.allClients.firstOrNull() ?: emptyList()
                val csvContent = CsvExporter.exportClientsToCsv(clients)
                shareExportFile(context, "clients_export.csv", "text/csv", csvContent)
                _message.value = Pair("Clients exported successfully (${clients.size} records)", false)
            } catch (e: Exception) {
                _message.value = Pair("Failed to export clients: ${e.message}", true)
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val json = backupManager.createBackupJson()
                val timeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                shareExportFile(context, "InvoiceForge_Backup_$timeStr.json", "application/json", json)
                _message.value = Pair("Backup created and shared successfully", false)
            } catch (e: Exception) {
                _message.value = Pair("Failed to create backup: ${e.message}", true)
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val result = backupManager.restoreBackupJson(jsonString)
            result.onSuccess { summary ->
                _message.value = Pair(
                    "Restored ${summary.invoicesCount} invoices, ${summary.clientsCount} clients, and ${summary.paymentsCount} payments.",
                    false
                )
            }.onFailure { e ->
                _message.value = Pair(e.message ?: "Failed to restore backup", true)
            }
        }
    }

    private fun shareExportFile(context: Context, filename: String, mimeType: String, content: String) {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "Share $filename").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    fun dismissMessage() {
        _message.value = null
    }

    class Factory(
        private val profileRepository: BusinessProfileRepository,
        private val preferencesManager: UserPreferencesManager,
        private val invoiceRepository: InvoiceRepository,
        private val clientRepository: ClientRepository,
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                profileRepository,
                preferencesManager,
                invoiceRepository,
                clientRepository,
                backupManager
            ) as T
        }
    }
}
