package com.invoiceforge.app.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.domain.model.InvoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InvoiceSortOrder(val displayName: String) {
    ISSUE_DATE_DESC("Date (Newest)"),
    ISSUE_DATE_ASC("Date (Oldest)"),
    DUE_DATE_ASC("Due Date (Soonest)"),
    TOTAL_DESC("Amount (High to Low)"),
    CLIENT_NAME_ASC("Client (A-Z)")
}

data class InvoiceListUiState(
    val invoices: List<InvoiceWithDetails> = emptyList(),
    val filteredInvoices: List<InvoiceWithDetails> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: InvoiceStatus? = null,
    val sortOrder: InvoiceSortOrder = InvoiceSortOrder.ISSUE_DATE_DESC,
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val isLoading: Boolean = true
)

class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<InvoiceStatus?>(null)
    private val _sortOrder = MutableStateFlow(InvoiceSortOrder.ISSUE_DATE_DESC)

    val uiState: StateFlow<InvoiceListUiState> = combine(
        invoiceRepository.allInvoices,
        profileRepository.profile,
        _searchQuery,
        _selectedStatus,
        _sortOrder
    ) { allInvoices, profile, query, status, sort ->
        val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")

        var filtered = allInvoices

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { item ->
                item.invoice.invoiceNumber.lowercase().contains(q) ||
                    (item.client?.name?.lowercase()?.contains(q) == true) ||
                    (item.client?.company?.lowercase()?.contains(q) == true) ||
                    item.invoice.notes.lowercase().contains(q)
            }
        }

        if (status != null) {
            filtered = filtered.filter { item ->
                item.calculate().status == status
            }
        }

        filtered = when (sort) {
            InvoiceSortOrder.ISSUE_DATE_DESC -> filtered.sortedByDescending { it.invoice.issueDate }
            InvoiceSortOrder.ISSUE_DATE_ASC -> filtered.sortedBy { it.invoice.issueDate }
            InvoiceSortOrder.DUE_DATE_ASC -> filtered.sortedBy { it.invoice.dueDate }
            InvoiceSortOrder.TOTAL_DESC -> filtered.sortedByDescending { it.calculate().grandTotal }
            InvoiceSortOrder.CLIENT_NAME_ASC -> filtered.sortedBy { it.client?.name.orEmpty().lowercase() }
        }

        InvoiceListUiState(
            invoices = allInvoices,
            filteredInvoices = filtered,
            searchQuery = query,
            selectedStatus = status,
            sortOrder = sort,
            currency = currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterSelected(status: InvoiceStatus?) {
        _selectedStatus.value = status
    }

    fun onSortOrderSelected(order: InvoiceSortOrder) {
        _sortOrder.value = order
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoiceId)
        }
    }

    fun duplicateInvoice(invoiceId: Long, onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val nextNumber = invoiceRepository.generateNextInvoiceNumber()
            val newId = invoiceRepository.duplicateInvoice(invoiceId, nextNumber)
            onDuplicated(newId)
        }
    }

    class Factory(
        private val invoiceRepository: InvoiceRepository,
        private val profileRepository: BusinessProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceListViewModel(invoiceRepository, profileRepository) as T
        }
    }
}
