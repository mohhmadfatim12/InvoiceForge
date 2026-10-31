package com.invoiceforge.app.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.data.repository.ClientRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditClientUiState(
    val clientId: Long = 0L,
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AddEditClientViewModel(
    private val clientId: Long,
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditClientUiState(clientId = clientId))
    val uiState: StateFlow<AddEditClientUiState> = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Long>()
    val saveSuccessEvent: SharedFlow<Long> = _saveSuccessEvent.asSharedFlow()

    init {
        if (clientId > 0) {
            viewModelScope.launch {
                val client = clientRepository.getClientSync(clientId)
                if (client != null) {
                    _uiState.update {
                        it.copy(
                            clientId = client.id,
                            name = client.name,
                            company = client.company,
                            phone = client.phone,
                            email = client.email,
                            address = client.address,
                            notes = client.notes,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onCompanyChanged(company: String) = _uiState.update { it.copy(company = company) }
    fun onPhoneChanged(phone: String) = _uiState.update { it.copy(phone = phone) }
    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email) }
    fun onAddressChanged(address: String) = _uiState.update { it.copy(address = address) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun saveClient() {
        val s = _uiState.value
        if (s.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Client name cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                val entity = ClientEntity(
                    id = s.clientId,
                    name = s.name.trim(),
                    company = s.company.trim(),
                    phone = s.phone.trim(),
                    email = s.email.trim(),
                    address = s.address.trim(),
                    notes = s.notes.trim()
                )
                val id = clientRepository.saveClient(entity)
                _saveSuccessEvent.emit(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save client: ${e.message}") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    class Factory(
        private val clientId: Long,
        private val clientRepository: ClientRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditClientViewModel(clientId, clientRepository) as T
        }
    }
}
