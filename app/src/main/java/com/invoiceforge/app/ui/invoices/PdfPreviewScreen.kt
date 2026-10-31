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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invoiceforge.app.ui.components.StatusBadge
import com.invoiceforge.app.ui.theme.NavyPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    viewModel: InvoiceDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val item = state.invoiceWithDetails
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateAndSharePdf() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateAndSharePdf() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Official A4 PDF Invoice")
                    }
                }
            }
        },
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
                Text("Invoice not found.")
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
                // PDF Generated Status Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Ready to Share",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "High-resolution standard A4 offline PDF is generated.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // A4 Document Preview Card (Simulates A4 paper look)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Top Accent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(NavyPrimary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Business Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.businessProfile?.name?.ifBlank { "InvoiceForge Studio" } ?: "InvoiceForge Studio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyPrimary
                                    )
                                    if (!state.businessProfile?.address.isNullOrBlank()) {
                                        Text(state.businessProfile!!.address, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    if (!state.businessProfile?.email.isNullOrBlank()) {
                                        Text(state.businessProfile!!.email, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("INVOICE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrimary)
                                    Text(item.invoice.invoiceNumber, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.DarkGray)
                                    Text("Due: ${dateFormat.format(Date(item.invoice.dueDate))}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Billed To
                            Text("BILLED TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(
                                text = item.client?.name?.ifBlank { "Guest Client" } ?: "Guest Client",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            if (!item.client?.company.isNullOrBlank()) {
                                Text(item.client!!.company, fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Items Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF4F6F8))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }

                            // Items Table Rows
                            item.items.forEach { line ->
                                val lineTotal = com.invoiceforge.app.domain.calculator.InvoiceCalculator
                                    .calculateLineTotal(line.quantity, line.unitPrice, line.discount)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${line.description} (x${line.quantity})",
                                        fontSize = 11.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = state.currency.formatAmount(lineTotal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Divider(color = Color(0xFFF8F8F8))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Totals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Subtotal: ${state.currency.formatAmount(calc.subtotal)}", fontSize = 11.sp, color = Color.DarkGray)
                                    if (calc.discountAmount > java.math.BigDecimal.ZERO) {
                                        Text("Discount: -${state.currency.formatAmount(calc.discountAmount)}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    if (calc.taxAmount > java.math.BigDecimal.ZERO) {
                                        Text("Tax: +${state.currency.formatAmount(calc.taxAmount)}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total Due: ${state.currency.formatAmount(calc.grandTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyPrimary
                                    )
                                    if (calc.amountPaid > java.math.BigDecimal.ZERO) {
                                        Text("Paid: ${state.currency.formatAmount(calc.amountPaid)}", fontSize = 11.sp, color = Color.DarkGray)
                                        Text(
                                            "Balance Due: ${state.currency.formatAmount(calc.balanceDue)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
