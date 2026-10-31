package com.invoiceforge.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.invoiceforge.app.data.local.preferences.ThemeMode
import com.invoiceforge.app.domain.model.CurrencyConfig
import com.invoiceforge.app.ui.components.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    var businessName by remember(state.businessProfile) { mutableStateOf(state.businessProfile.name) }
    var address by remember(state.businessProfile) { mutableStateOf(state.businessProfile.address) }
    var phone by remember(state.businessProfile) { mutableStateOf(state.businessProfile.phone) }
    var email by remember(state.businessProfile) { mutableStateOf(state.businessProfile.email) }
    var currency by remember(state.businessProfile) { mutableStateOf(state.businessProfile.currency) }
    var taxLabel by remember(state.businessProfile) { mutableStateOf(state.businessProfile.taxLabel) }
    var defaultTaxRate by remember(state.businessProfile) { mutableStateOf(state.businessProfile.defaultTaxRate.toString()) }
    var invoiceNotes by remember(state.businessProfile) { mutableStateOf(state.businessProfile.invoiceNotes) }
    var paymentTerms by remember(state.businessProfile) { mutableStateOf(state.businessProfile.paymentTerms) }
    var invoicePrefix by remember(state.invoicePrefix) { mutableStateOf(state.invoicePrefix) }

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var showRestoreConfirmWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateBusinessProfile(
                                name = businessName,
                                address = address,
                                phone = phone,
                                email = email,
                                currency = currency,
                                taxLabel = taxLabel,
                                defaultTaxRate = defaultTaxRate.toDoubleOrNull() ?: 0.0,
                                invoiceNotes = invoiceNotes,
                                paymentTerms = paymentTerms
                            )
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                // Section: Business Profile
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Business Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "This information will automatically appear on all generated PDF invoices.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business / Freelancer Name *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Business Address") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Phone") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Currency Selector Dropdown
                            ExposedDropdownMenuBox(
                                expanded = currencyDropdownExpanded,
                                onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded }
                            ) {
                                val currentConfig = CurrencyConfig.findByCode(currency)
                                OutlinedTextField(
                                    value = "${currentConfig.code} (${currentConfig.symbol} - ${currentConfig.name})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Default Currency") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = currencyDropdownExpanded,
                                    onDismissRequest = { currencyDropdownExpanded = false }
                                ) {
                                    CurrencyConfig.POPULAR_CURRENCIES.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text("${item.code} - ${item.name} (${item.symbol})") },
                                            onClick = {
                                                currency = item.code
                                                currencyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = taxLabel,
                                    onValueChange = { taxLabel = it },
                                    label = { Text("Tax Label (e.g. Tax, VAT)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )

                                OutlinedTextField(
                                    value = defaultTaxRate,
                                    onValueChange = { defaultTaxRate = it },
                                    label = { Text("Default Tax %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = paymentTerms,
                                onValueChange = { paymentTerms = it },
                                label = { Text("Default Payment Terms (e.g. Net 14)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = invoiceNotes,
                                onValueChange = { invoiceNotes = it },
                                label = { Text("Default Invoice Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.updateBusinessProfile(
                                        name = businessName,
                                        address = address,
                                        phone = phone,
                                        email = email,
                                        currency = currency,
                                        taxLabel = taxLabel,
                                        defaultTaxRate = defaultTaxRate.toDoubleOrNull() ?: 0.0,
                                        invoiceNotes = invoiceNotes,
                                        paymentTerms = paymentTerms
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Business Details")
                            }
                        }
                    }
                }

                // Section: App Appearance & Preferences
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Appearance & Numbering", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Text("App Theme", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.themeMode == ThemeMode.SYSTEM,
                                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                    label = { Text("System") }
                                )
                                FilterChip(
                                    selected = state.themeMode == ThemeMode.LIGHT,
                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                    label = { Text("Light") }
                                )
                                FilterChip(
                                    selected = state.themeMode == ThemeMode.DARK,
                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                    label = { Text("Dark") }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = invoicePrefix,
                                    onValueChange = { invoicePrefix = it },
                                    label = { Text("Invoice Prefix") },
                                    placeholder = { Text("INV-") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { viewModel.setInvoicePrefix(invoicePrefix) }
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }

                // Section: Data Export & Backup
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Data Management & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Your data is stored 100% locally. Export backups or spreadsheets at any time.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // CSV Export buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.exportInvoicesCsv(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Invoices CSV")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.exportClientsCsv(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clients CSV")
                                }
                            }

                            // Full Backup & Restore
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.exportBackup(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Backup")
                                }
                                OutlinedButton(
                                    onClick = { showRestoreDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Backup")
                                }
                            }
                        }
                    }
                }

                // Section: About App
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("About InvoiceForge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Version 1.0.0 • Offline Invoice Maker & PDF Generator", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "100% Offline • No accounts, subscriptions, or external servers required. Room DB + Jetpack Compose + Native Android PDF.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Restore Backup Dialog (Input JSON)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste your InvoiceForge JSON backup content below to restore your invoices and clients.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        label = { Text("Backup JSON Content") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            showRestoreDialog = false
                            showRestoreConfirmWarning = true
                        }
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Destructive Restore Confirmation Warning
    if (showRestoreConfirmWarning) {
        ConfirmationDialog(
            title = "Warning: Overwrite Existing Data?",
            message = "Restoring this backup will completely replace all existing invoices, clients, items, and recorded payments in your app database. This cannot be undone.",
            confirmButtonText = "Restore & Overwrite",
            isDestructive = true,
            onConfirm = {
                viewModel.restoreBackup(restoreJsonText)
                restoreJsonText = ""
                showRestoreConfirmWarning = false
            },
            onDismiss = { showRestoreConfirmWarning = false }
        )
    }
}
