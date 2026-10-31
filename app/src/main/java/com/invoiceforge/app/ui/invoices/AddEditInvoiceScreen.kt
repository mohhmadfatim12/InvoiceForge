package com.invoiceforge.app.ui.invoices

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.TaxType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvoiceScreen(
    viewModel: AddEditInvoiceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateClient: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collect { id ->
            onSaveSuccess(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.invoiceId > 0) "Edit Invoice" else "New Invoice",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveInvoice() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Invoice")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Live Summary Bar
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.currency.formatAmount(state.grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.saveInvoice() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Invoice")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Invoice Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Invoice Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = state.invoiceNumber,
                                onValueChange = { viewModel.onInvoiceNumberChanged(it) },
                                label = { Text("Invoice Number *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Client Dropdown
                            var clientExpanded by remember { mutableStateOf(false) }
                            val selectedClient = state.clients.firstOrNull { it.id == state.selectedClientId }

                            ExposedDropdownMenuBox(
                                expanded = clientExpanded,
                                onExpandedChange = { clientExpanded = !clientExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedClient?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Client *") },
                                    placeholder = { Text("Choose a client") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = clientExpanded,
                                    onDismissRequest = { clientExpanded = false }
                                ) {
                                    if (state.clients.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No clients yet. Tap here to add.") },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                                            onClick = {
                                                clientExpanded = false
                                                onNavigateToCreateClient()
                                            }
                                        )
                                    } else {
                                        state.clients.forEach { client ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(client.name, fontWeight = FontWeight.Bold)
                                                        if (client.company.isNotBlank()) {
                                                            Text(client.company, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.onClientSelected(client.id)
                                                    clientExpanded = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("+ Add New Client", color = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                clientExpanded = false
                                                onNavigateToCreateClient()
                                            }
                                        )
                                    }
                                }
                            }

                            // Dates Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Issue Date
                                OutlinedTextField(
                                    value = dateFormat.format(Date(state.issueDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Issue Date") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val cal = Calendar.getInstance().apply { timeInMillis = state.issueDate }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val picked = Calendar.getInstance().apply { set(y, m, d) }
                                                    viewModel.onIssueDateChanged(picked.timeInMillis)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick issue date")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                // Due Date
                                OutlinedTextField(
                                    value = dateFormat.format(Date(state.dueDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due Date") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val cal = Calendar.getInstance().apply { timeInMillis = state.dueDate }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val picked = Calendar.getInstance().apply { set(y, m, d) }
                                                    viewModel.onDueDateChanged(picked.timeInMillis)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick due date")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Section: Line Items Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Line Items (${state.items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { viewModel.addLineItem() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
                }

                // Line Items List
                itemsIndexed(state.items) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Item #${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (state.items.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removeLineItem(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove item",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { viewModel.updateLineItem(index, description = it) },
                                label = { Text("Description *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.quantity,
                                    onValueChange = { viewModel.updateLineItem(index, quantity = it) },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = item.unitPrice,
                                    onValueChange = { viewModel.updateLineItem(index, unitPrice = it) },
                                    label = { Text("Unit Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )

                                OutlinedTextField(
                                    value = item.discount,
                                    onValueChange = { viewModel.updateLineItem(index, discount = it) },
                                    label = { Text("Discount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Line Total: ${state.currency.formatAmount(item.calculateLineTotal())}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Section: Discount, Tax, Shipping
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Discount & Tax", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            // Discount
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Discount", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = state.discountType == DiscountType.PERCENTAGE,
                                            onClick = { viewModel.onDiscountTypeChanged(DiscountType.PERCENTAGE) },
                                            label = { Text("%") }
                                        )
                                        FilterChip(
                                            selected = state.discountType == DiscountType.FIXED,
                                            onClick = { viewModel.onDiscountTypeChanged(DiscountType.FIXED) },
                                            label = { Text(state.currency.symbol) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = state.discountValue,
                                    onValueChange = { viewModel.onDiscountValueChanged(it) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Tax
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tax Rate", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = state.taxType == TaxType.PERCENTAGE,
                                            onClick = { viewModel.onTaxTypeChanged(TaxType.PERCENTAGE) },
                                            label = { Text("%") }
                                        )
                                        FilterChip(
                                            selected = state.taxType == TaxType.FIXED,
                                            onClick = { viewModel.onTaxTypeChanged(TaxType.FIXED) },
                                            label = { Text(state.currency.symbol) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = state.taxRateOrAmount,
                                    onValueChange = { viewModel.onTaxRateOrAmountChanged(it) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Shipping
                            OutlinedTextField(
                                value = state.shipping,
                                onValueChange = { viewModel.onShippingChanged(it) },
                                label = { Text("Shipping Fee") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Section: Notes & Terms
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Notes & Payment Terms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = state.paymentTerms,
                                onValueChange = { viewModel.onPaymentTermsChanged(it) },
                                label = { Text("Payment Terms") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.notes,
                                onValueChange = { viewModel.onNotesChanged(it) },
                                label = { Text("Notes (visible on invoice)") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Spacing at bottom for summary bar
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
