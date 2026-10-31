package com.invoiceforge.app.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.ClientRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.ClientSummary
import com.invoiceforge.app.domain.model.CurrencyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientDetailUiState(
    val client: ClientEntity? = null,
    val summary: ClientSummary? = null,
    val invoices: List<InvoiceWithDetails> = emptyList(),
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val isLoading: Boolean = true
)

class ClientDetailViewModel(
    private val clientId: Long,
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    val uiState: StateFlow<ClientDetailUiState> = combine(
        clientRepository.getClient(clientId),
        clientRepository.getClientSummary(clientId),
        invoiceRepository.getInvoicesForClient(clientId),
        profileRepository.profile
    ) { client, summary, invoices, profile ->
        val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")
        ClientDetailUiState(
            client = client,
            summary = summary,
            invoices = invoices,
            currency = currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientDetailUiState(isLoading = true)
    )

    fun deleteClient(onDeleted: () -> Unit) {
        val currentClient = uiState.value.client ?: return
        viewModelScope.launch {
            clientRepository.deleteClient(currentClient)
            onDeleted()
        }
    }

    class Factory(
        private val clientId: Long,
        private val clientRepository: ClientRepository,
        private val invoiceRepository: InvoiceRepository,
        private val profileRepository: BusinessProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientDetailViewModel(
                clientId,
                clientRepository,
                invoiceRepository,
                profileRepository
            ) as T
        }
    }
}
