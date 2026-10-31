package com.invoiceforge.app.ui.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.local.entity.InvoiceEntity
import com.invoiceforge.app.data.local.entity.InvoiceItemEntity
import com.invoiceforge.app.data.repository.BusinessProfileRepository
import com.invoiceforge.app.data.repository.ClientRepository
import com.invoiceforge.app.data.repository.InvoiceRepository
import com.invoiceforge.app.domain.calculator.InvoiceCalculator
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.domain.model.TaxType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class LineItemFormState(
    val id: Long = 0L,
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0.00",
    val discount: String = "0.00"
) {
    fun calculateLineTotal(): BigDecimal {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val price = unitPrice.toDoubleOrNull() ?: 0.0
        val disc = discount.toDoubleOrNull() ?: 0.0
        return InvoiceCalculator.calculateLineTotal(qty, price, disc)
    }
}

data class AddEditInvoiceUiState(
    val invoiceId: Long = 0L,
    val invoiceNumber: String = "",
    val selectedClientId: Long? = null,
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val notes: String = "",
    val paymentTerms: String = "Net 14",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: String = "0",
    val taxType: TaxType = TaxType.PERCENTAGE,
    val taxRateOrAmount: String = "0",
    val shipping: String = "0",
    val items: List<LineItemFormState> = listOf(LineItemFormState()),
    val clients: List<ClientEntity> = emptyList(),
    val currency: CurrencyConfig = CurrencyConfig.DEFAULT,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val subtotal: BigDecimal
        get() = InvoiceCalculator.calculateSubtotal(items.map { it.calculateLineTotal() })

    val discountAmount: BigDecimal
        get() = InvoiceCalculator.calculateDiscountAmount(
            subtotal = subtotal,
            discountType = discountType,
            discountValue = discountValue.toDoubleOrNull() ?: 0.0
        )

    val taxAmount: BigDecimal
        get() = InvoiceCalculator.calculateTaxAmount(
            taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO),
            taxType = taxType,
            taxRateOrAmount = taxRateOrAmount.toDoubleOrNull() ?: 0.0
        )

    val grandTotal: BigDecimal
        get() = InvoiceCalculator.calculateGrandTotal(
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            shipping = shipping.toDoubleOrNull() ?: 0.0
        )
}

class AddEditInvoiceViewModel(
    private val invoiceId: Long,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val profileRepository: BusinessProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditInvoiceUiState(invoiceId = invoiceId))
    val uiState: StateFlow<AddEditInvoiceUiState> = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Long>()
    val saveSuccessEvent: SharedFlow<Long> = _saveSuccessEvent.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val clients = clientRepository.allClients.firstOrNull() ?: emptyList()
            val profile = profileRepository.profile.firstOrNull()
            val currency = CurrencyConfig.findByCode(profile?.currency ?: "USD")

            if (invoiceId > 0) {
                val existing = invoiceRepository.getInvoiceByIdSync(invoiceId)
                if (existing != null) {
                    val inv = existing.invoice
                    _uiState.update { current ->
                        current.copy(
                            invoiceId = inv.id,
                            invoiceNumber = inv.invoiceNumber,
                            selectedClientId = inv.clientId,
                            issueDate = inv.issueDate,
                            dueDate = inv.dueDate,
                            status = inv.status,
                            notes = inv.notes,
                            paymentTerms = inv.paymentTerms,
                            discountType = inv.discountType,
                            discountValue = if (inv.discountValue > 0) inv.discountValue.toString() else "0",
                            taxType = inv.taxType,
                            taxRateOrAmount = if (inv.taxRateOrAmount > 0) inv.taxRateOrAmount.toString() else "0",
                            shipping = if (inv.shipping > 0) inv.shipping.toString() else "0",
                            items = existing.items.map {
                                LineItemFormState(
                                    id = it.id,
                                    description = it.description,
                                    quantity = it.quantity.toString(),
                                    unitPrice = it.unitPrice.toString(),
                                    discount = it.discount.toString()
                                )
                            }.ifEmpty { listOf(LineItemFormState()) },
                            clients = clients,
                            currency = currency,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            // New invoice defaults
            val nextNumber = invoiceRepository.generateNextInvoiceNumber()
            val defaultTax = profile?.defaultTaxRate ?: 0.0
            val defaultNotes = profile?.invoiceNotes.orEmpty()
            val defaultTerms = profile?.paymentTerms.orEmpty().ifBlank { "Net 14" }

            _uiState.update { current ->
                current.copy(
                    invoiceNumber = nextNumber,
                    taxRateOrAmount = if (defaultTax > 0) defaultTax.toString() else "0",
                    notes = defaultNotes,
                    paymentTerms = defaultTerms,
                    clients = clients,
                    currency = currency,
                    isLoading = false
                )
            }
        }
    }

    fun onInvoiceNumberChanged(num: String) = _uiState.update { it.copy(invoiceNumber = num) }
    fun onClientSelected(clientId: Long) = _uiState.update { it.copy(selectedClientId = clientId) }
    fun onIssueDateChanged(date: Long) = _uiState.update { it.copy(issueDate = date) }
    fun onDueDateChanged(date: Long) = _uiState.update { it.copy(dueDate = date) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun onPaymentTermsChanged(terms: String) = _uiState.update { it.copy(paymentTerms = terms) }

    fun onDiscountTypeChanged(type: DiscountType) = _uiState.update { it.copy(discountType = type) }
    fun onDiscountValueChanged(value: String) = _uiState.update { it.copy(discountValue = value) }

    fun onTaxTypeChanged(type: TaxType) = _uiState.update { it.copy(taxType = type) }
    fun onTaxRateOrAmountChanged(value: String) = _uiState.update { it.copy(taxRateOrAmount = value) }

    fun onShippingChanged(shipping: String) = _uiState.update { it.copy(shipping = shipping) }

    fun addLineItem() {
        _uiState.update {
            it.copy(items = it.items + LineItemFormState())
        }
    }

    fun removeLineItem(index: Int) {
        _uiState.update { current ->
            if (current.items.size > 1) {
                current.copy(items = current.items.filterIndexed { i, _ -> i != index })
            } else {
                current
            }
        }
    }

    fun updateLineItem(
        index: Int,
        description: String? = null,
        quantity: String? = null,
        unitPrice: String? = null,
        discount: String? = null
    ) {
        _uiState.update { current ->
            val updated = current.items.toMutableList()
            if (index in updated.indices) {
                val item = updated[index]
                updated[index] = item.copy(
                    description = description ?: item.description,
                    quantity = quantity ?: item.quantity,
                    unitPrice = unitPrice ?: item.unitPrice,
                    discount = discount ?: item.discount
                )
            }
            current.copy(items = updated)
        }
    }

    fun saveInvoice() {
        val s = _uiState.value
        if (s.invoiceNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invoice number cannot be empty") }
            return
        }
        if (s.selectedClientId == null || s.selectedClientId <= 0) {
            _uiState.update { it.copy(errorMessage = "Please select a client") }
            return
        }
        val validItems = s.items.filter { it.description.isNotBlank() }
        if (validItems.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least one line item with a description") }
            return
        }

        viewModelScope.launch {
            try {
                val invoiceEntity = InvoiceEntity(
                    id = s.invoiceId,
                    invoiceNumber = s.invoiceNumber.trim(),
                    clientId = s.selectedClientId,
                    issueDate = s.issueDate,
                    dueDate = s.dueDate,
                    status = s.status,
                    notes = s.notes.trim(),
                    paymentTerms = s.paymentTerms.trim(),
                    discountType = s.discountType,
                    discountValue = s.discountValue.toDoubleOrNull() ?: 0.0,
                    taxType = s.taxType,
                    taxRateOrAmount = s.taxRateOrAmount.toDoubleOrNull() ?: 0.0,
                    shipping = s.shipping.toDoubleOrNull() ?: 0.0
                )

                val itemEntities = validItems.mapIndexed { index, item ->
                    InvoiceItemEntity(
                        id = item.id,
                        invoiceId = s.invoiceId,
                        description = item.description.trim(),
                        quantity = item.quantity.toDoubleOrNull() ?: 1.0,
                        unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0,
                        discount = item.discount.toDoubleOrNull() ?: 0.0,
                        orderIndex = index
                    )
                }

                val savedId = invoiceRepository.saveInvoice(invoiceEntity, itemEntities)
                _saveSuccessEvent.emit(savedId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    class Factory(
        private val invoiceId: Long,
        private val invoiceRepository: InvoiceRepository,
        private val clientRepository: ClientRepository,
        private val profileRepository: BusinessProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditInvoiceViewModel(
                invoiceId,
                invoiceRepository,
                clientRepository,
                profileRepository
            ) as T
        }
    }
}
