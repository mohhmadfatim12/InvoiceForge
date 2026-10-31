package com.invoiceforge.app.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.ClientRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.CurrencyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class ClientItemUiModel(
    val client: ClientEntity,
    val totalInvoiced: BigDecimal = BigDecimal.ZERO,
    val outstandingAmount: BigDecimal = BigDecimal.ZERO,
    val invoiceCount: Int = 0
)

data class ClientListUiState(
    val clients: List<ClientItemUiModel> = emptyList(),
    val filteredClients: List<ClientItemUiModel> = emptyList(),
    val searchQuery: String = "",
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val isLoading: Boolean = true
)

class ClientListViewModel(
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ClientListUiState> = combine(
        clientRepository.allClients,
        invoiceRepository.allInvoices,
        profileRepository.profile,
        _searchQuery
    ) { allClients, allInvoices, profile, query ->
        val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")

        val clientModels = allClients.map { client ->
            val clientInvoices = allInvoices.filter { it.invoice.clientId == client.id }
            var invoiced = BigDecimal.ZERO
            var paid = BigDecimal.ZERO

            clientInvoices.forEach { item ->
                val calc = item.calculate()
                invoiced = invoiced.add(calc.grandTotal)
                paid = paid.add(calc.amountPaid)
            }

            val outstanding = invoiced.subtract(paid).max(BigDecimal.ZERO)
            ClientItemUiModel(
                client = client,
                totalInvoiced = invoiced,
                outstandingAmount = outstanding,
                invoiceCount = clientInvoices.size
            )
        }

        val filtered = if (query.isBlank()) {
            clientModels
        } else {
            val q = query.trim().lowercase()
            clientModels.filter {
                it.client.name.lowercase().contains(q) ||
                    it.client.company.lowercase().contains(q) ||
                    it.client.email.lowercase().contains(q) ||
                    it.client.phone.contains(q)
            }
        }

        ClientListUiState(
            clients = clientModels,
            filteredClients = filtered,
            searchQuery = query,
            currency = currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    class Factory(
        private val clientRepository: ClientRepository,
        private val invoiceRepository: InvoiceRepository,
        private val profileRepository: BusinessProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientListViewModel(clientRepository, invoiceRepository, profileRepository) as T
        }
    }
}
