package com.invoiceforge.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import com.invoiceforge.app.data.local.entity.InvoiceWithDetails
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.domain.model.DashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStats = DashboardStats(),
    val recentInvoices: List<InvoiceWithDetails> = emptyList(),
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val businessProfile: BusinessProfileEntity? = null
)

class DashboardViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        invoiceRepository.getDashboardStats(),
        invoiceRepository.allInvoices,
        profileRepository.profile
    ) { stats, allInvoices, profile ->
        val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")
        val recents = allInvoices.take(5)
        DashboardUiState(
            isLoading = false,
            stats = stats,
            recentInvoices = recents,
            currency = currency,
            businessProfile = profile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    class Factory(
        private val invoiceRepository: InvoiceRepository,
        private val profileRepository: BusinessProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(invoiceRepository, profileRepository) as T
        }
    }
}
