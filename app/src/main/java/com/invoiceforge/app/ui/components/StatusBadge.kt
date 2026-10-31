package com.invoiceforge.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invoiceforge.app.domain.model.InvoiceStatus
import com.invoiceforge.app.ui.theme.StatusDraft
import com.invoiceforge.app.ui.theme.StatusOverdue
import com.invoiceforge.app.ui.theme.StatusPaid
import com.invoiceforge.app.ui.theme.StatusPartiallyPaid
import com.invoiceforge.app.ui.theme.StatusPending

@Composable
fun StatusBadge(
    status: InvoiceStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        InvoiceStatus.DRAFT -> Pair(StatusDraft.copy(alpha = 0.15f), StatusDraft)
        InvoiceStatus.PENDING -> Pair(StatusPending.copy(alpha = 0.15f), StatusPending)
        InvoiceStatus.PARTIALLY_PAID -> Pair(StatusPartiallyPaid.copy(alpha = 0.15f), StatusPartiallyPaid)
        InvoiceStatus.PAID -> Pair(StatusPaid.copy(alpha = 0.15f), StatusPaid)
        InvoiceStatus.OVERDUE -> Pair(StatusOverdue.copy(alpha = 0.15f), StatusOverdue)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
