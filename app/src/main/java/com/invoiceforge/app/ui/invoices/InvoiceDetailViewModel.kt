package com.invoiceforge.app.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.local.entity.PaymentEntity
import com.invoiceforge.app.data.pdf.PdfInvoiceGenerator
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class InvoiceDetailUiState(
    val invoiceWithDetails: InvoiceWithDetails? = null,
    val businessProfile: BusinessProfileEntity? = null,
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class InvoiceDetailViewModel(
    private val invoiceId: Long,
    private val invoiceRepository: InvoiceRepository,
    private val profileRepository: BusinessProfileRepository,
    private val pdfGenerator: PdfInvoiceGenerator
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvoiceDetailUiState> = combine(
        invoiceRepository.getInvoiceById(invoiceId),
        profileRepository.profile,
        _errorMessage
    ) { invoice, profile, error ->
        val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")
        InvoiceDetailUiState(
            invoiceWithDetails = invoice,
            businessProfile = profile,
            currency = currency,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceDetailUiState(isLoading = true)
    )

    fun recordPayment(amount: Double, method: PaymentMethod, note: String) {
        viewModelScope.launch {
            val result = invoiceRepository.recordPayment(
                invoiceId = invoiceId,
                amount = amount,
                method = method,
                note = note
            )
            result.onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to record payment"
            }
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            invoiceRepository.deletePayment(payment)
        }
    }

    fun deleteInvoice(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val success = invoiceRepository.deleteInvoice(invoiceId)
            if (success) onDeleted()
        }
    }

    fun generateAndSharePdf() {
        val details = uiState.value.invoiceWithDetails ?: return
        try {
            val file = pdfGenerator.generateInvoicePdf(details, uiState.value.businessProfile)
            pdfGenerator.sharePdf(file)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to generate PDF: ${e.message}"
        }
    }

    fun generatePdfFile(): File? {
        val details = uiState.value.invoiceWithDetails ?: return null
        return try {
            pdfGenerator.generateInvoicePdf(details, uiState.value.businessProfile)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to render PDF: ${e.message}"
            null
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val invoiceId: Long,
        private val invoiceRepository: InvoiceRepository,
        private val profileRepository: BusinessProfileRepository,
        private val pdfGenerator: PdfInvoiceGenerator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceDetailViewModel(
                invoiceId,
                invoiceRepository,
                profileRepository,
                pdfGenerator
            ) as T
        }
    }
}
