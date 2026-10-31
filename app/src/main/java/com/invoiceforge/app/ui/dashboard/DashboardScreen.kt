package com.invoiceforge.app.ui.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.invoiceforge.app.domain.model.MonthlyRevenue
import com.invoiceforge.app.ui.components.EmptyStateView
import com.invoiceforge.app.ui.components.StatCard
import com.invoiceforge.app.ui.components.StatusBadge
import com.invoiceforge.app.ui.theme.NavyPrimary
import com.invoiceforge.app.ui.theme.StatusOverdue
import com.invoiceforge.app.ui.theme.StatusPaid
import com.invoiceforge.app.ui.theme.StatusPartiallyPaid
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateInvoiceClick: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onViewAllInvoicesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.businessProfile?.name?.ifBlank { "InvoiceForge" } ?: "InvoiceForge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offline Invoicing & Reports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val initials = (state.businessProfile?.name ?: "IF")
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                        .ifBlank { "IF" }

                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateInvoiceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Invoice")
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Stats Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Total Invoiced",
                                value = state.currency.formatAmount(state.stats.totalInvoiced),
                                subtitle = "${state.stats.invoicesThisMonthCount} this month",
                                icon = Icons.Default.AttachMoney,
                                iconTint = NavyPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Total Received",
                                value = state.currency.formatAmount(state.stats.totalReceived),
                                subtitle = "${state.stats.paidCount} paid in full",
                                icon = Icons.Default.CheckCircle,
                                iconTint = StatusPaid,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Outstanding",
                                value = state.currency.formatAmount(state.stats.totalOutstanding),
                                subtitle = "${state.stats.partiallyPaidCount} partially paid",
                                icon = Icons.Default.PendingActions,
                                iconTint = StatusPartiallyPaid,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Overdue",
                                value = "${state.stats.overdueCount}",
                                subtitle = "Action needed",
                                icon = Icons.Default.ErrorOutline,
                                iconTint = StatusOverdue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 6-Month Trend Chart
                if (state.stats.monthlyTrends.isNotEmpty()) {
                    item {
                        MonthlyTrendChartCard(
                            trends = state.stats.monthlyTrends,
                            currency = state.currency
                        )
                    }
                }

                // Recent Invoices Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Invoices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.recentInvoices.isNotEmpty()) {
                            TextButton(onClick = onViewAllInvoicesClick) {
                                Text("View All")
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Recent Invoices List
                if (state.recentInvoices.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = "No Invoices Yet",
                            description = "Create your first professional offline invoice in seconds.",
                            buttonText = "New Invoice",
                            onButtonClick = onCreateInvoiceClick
                        )
                    }
                } else {
                    items(state.recentInvoices, key = { it.invoice.id }) { item ->
                        val calc = item.calculate()
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInvoiceClick(item.invoice.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.invoice.invoiceNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(status = calc.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.client?.name?.ifBlank { "No client specified" } ?: "No client specified",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Due: ${dateFormat.format(Date(item.invoice.dueDate))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = state.currency.formatAmount(calc.grandTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (calc.balanceDue > BigDecimal.ZERO && calc.balanceDue < calc.grandTotal) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Bal: ${state.currency.formatAmount(calc.balanceDue)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendChartCard(
    trends: List<MonthlyRevenue>,
    currency: com.invoiceforge.app.domain.model.CurrencyConfig,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6-Month Revenue Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NavyPrimary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invoiced", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusPaid))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val maxVal = trends.maxOfOrNull {
                maxOf(it.invoicedAmount.toDouble(), it.receivedAmount.toDouble())
            } ?: 100.0
            val safeMax = if (maxVal <= 0.0) 100.0 else maxVal

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trends.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            val invRatio = (item.invoicedAmount.toDouble() / safeMax).toFloat().coerceIn(0.04f, 1f)
                            val recRatio = (item.receivedAmount.toDouble() / safeMax).toFloat().coerceIn(0.04f, 1f)

                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxSize(invRatio)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(NavyPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxSize(recRatio)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(StatusPaid)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
