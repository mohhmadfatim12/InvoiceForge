package com.invoiceforge.app.ui.invoices

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invoiceforge.app.data.local.entity.PaymentEntity
import com.invoiceforge.app.domain.model.DiscountType
import com.invoiceforge.app.domain.model.PaymentMethod
import com.invoiceforge.app.domain.model.TaxType
import com.invoiceforge.app.ui.components.ConfirmationDialog
import com.invoiceforge.app.ui.components.StatusBadge
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    viewModel: InvoiceDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onViewPdfClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val item = state.invoiceWithDetails

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item?.invoice?.invoiceNumber ?: "Invoice Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateAndSharePdf() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                    }
                    if (item != null) {
                        IconButton(onClick = { onEditClick(item.invoice.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Invoice")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Invoice", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (item != null) {
                val calc = item.calculate()
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onViewPdfClick(item.invoice.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Preview")
                        }

                        if (calc.balanceDue > BigDecimal.ZERO) {
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Payment")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.generateAndSharePdf() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PDF")
                            }
                        }
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
        } else if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Invoice not found or deleted.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val calc = item.calculate()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Details Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.invoice.invoiceNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Issued: ${dateFormat.format(Date(item.invoice.issueDate))}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = calc.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("BILLED TO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.client?.name?.ifBlank { "No client selected" } ?: "No client selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!item.client?.company.isNullOrBlank()) {
                                Text(item.client!!.company, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!item.client?.email.isNullOrBlank()) {
                                Text(item.client!!.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Due Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        dateFormat.format(Date(item.invoice.dueDate)),
                                        fontWeight = FontWeight.Medium,
                                        color = if (calc.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (item.invoice.paymentTerms.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Payment Terms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(item.invoice.paymentTerms, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Line Items Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Items (${item.items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            item.items.forEachIndexed { index, line ->
                                val lineTotal = com.invoiceforge.app.domain.calculator.InvoiceCalculator
                                    .calculateLineTotal(line.quantity, line.unitPrice, line.discount)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.description, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${line.quantity} × ${state.currency.formatAmount(line.unitPrice)}${if (line.discount > 0) " (-${state.currency.formatAmount(line.discount)})" else ""}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = state.currency.formatAmount(lineTotal),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (index < item.items.lastIndex) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                // Financial Breakdown Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            BreakdownRow("Subtotal", state.currency.formatAmount(calc.subtotal))

                            if (calc.discountAmount > BigDecimal.ZERO) {
                                val label = if (item.invoice.discountType == DiscountType.PERCENTAGE) {
                                    "Discount (${item.invoice.discountValue}%)"
                                } else "Discount"
                                BreakdownRow(label, "-${state.currency.formatAmount(calc.discountAmount)}")
                            }

                            if (calc.taxAmount > BigDecimal.ZERO) {
                                val label = if (item.invoice.taxType == TaxType.PERCENTAGE) {
                                    "Tax (${item.invoice.taxRateOrAmount}%)"
                                } else "Tax"
                                BreakdownRow(label, "+${state.currency.formatAmount(calc.taxAmount)}")
                            }

                            if (item.invoice.shipping > 0) {
                                BreakdownRow("Shipping", "+${state.currency.formatAmount(item.invoice.shipping)}")
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    state.currency.formatAmount(calc.grandTotal),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (calc.amountPaid > BigDecimal.ZERO) {
                                BreakdownRow("Amount Paid", state.currency.formatAmount(calc.amountPaid))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Balance Due", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(
                                        state.currency.formatAmount(calc.balanceDue),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Payments History Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Payments (${item.payments.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (calc.balanceDue > BigDecimal.ZERO) {
                                    TextButton(onClick = { showPaymentDialog = true }) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record")
                                    }
                                }
                            }

                            if (item.payments.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No payments recorded yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                item.payments.forEach { pay ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${pay.paymentMethod.displayName} • ${dateFormat.format(Date(pay.paymentDate))}",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (pay.note.isNotBlank()) {
                                                Text(pay.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = state.currency.formatAmount(pay.amount),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { paymentToDelete = pay },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete payment",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes Card
                if (item.invoice.notes.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(item.invoice.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Record Payment Dialog
    if (showPaymentDialog && item != null) {
        val calc = item.calculate()
        var paymentAmountStr by remember { mutableStateOf(calc.balanceDue.toString()) }
        var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
        var paymentNote by remember { mutableStateOf("") }
        var paymentError by remember { mutableStateOf<String?>(null) }
        var methodDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Outstanding Balance: ${state.currency.formatAmount(calc.balanceDue)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = {
                            paymentAmountStr = it
                            paymentError = null
                        },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = paymentError != null,
                        supportingText = paymentError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = methodDropdownExpanded,
                        onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMethod.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.displayName) },
                                    onClick = {
                                        selectedMethod = method
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmountStr.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            paymentError = "Enter a valid payment amount"
                            return@Button
                        }
                        if (BigDecimal.valueOf(amt) > calc.balanceDue) {
                            paymentError = "Payment cannot exceed balance due (${state.currency.formatAmount(calc.balanceDue)})"
                            return@Button
                        }
                        viewModel.recordPayment(amt, selectedMethod, paymentNote)
                        showPaymentDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Invoice Confirmation
    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Invoice",
            message = "Are you sure you want to delete invoice ${item?.invoice?.invoiceNumber}? All line items and recorded payments will be deleted.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteInvoice(onDeleted = onNavigateBack)
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Delete Payment Confirmation
    paymentToDelete?.let { pay ->
        ConfirmationDialog(
            title = "Delete Payment",
            message = "Are you sure you want to delete this payment of ${state.currency.formatAmount(pay.amount)}? The invoice balance will be restored.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deletePayment(pay)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
