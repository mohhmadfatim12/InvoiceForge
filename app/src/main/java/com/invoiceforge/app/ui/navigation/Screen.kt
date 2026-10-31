package com.invoiceforge.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Invoices : Screen("invoices")
    data object AddEditInvoice : Screen("add_edit_invoice?invoiceId={invoiceId}") {
        fun createRoute(invoiceId: Long? = null): String {
            return if (invoiceId != null && invoiceId > 0) {
                "add_edit_invoice?invoiceId=$invoiceId"
            } else {
                "add_edit_invoice"
            }
        }
    }
    data object InvoiceDetail : Screen("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: Long): String = "invoice_detail/$invoiceId"
    }
    data object PdfPreview : Screen("pdf_preview/{invoiceId}") {
        fun createRoute(invoiceId: Long): String = "pdf_preview/$invoiceId"
    }

    data object Clients : Screen("clients")
    data object AddEditClient : Screen("add_edit_client?clientId={clientId}") {
        fun createRoute(clientId: Long? = null): String {
            return if (clientId != null && clientId > 0) {
                "add_edit_client?clientId=$clientId"
            } else {
                "add_edit_client"
            }
        }
    }
    data object ClientDetail : Screen("client_detail/{clientId}") {
        fun createRoute(clientId: Long): String = "client_detail/$clientId"
    }

    data object Settings : Screen("settings")
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Home)
    data object Invoices : BottomNavItem(Screen.Invoices.route, "Invoices", Icons.Default.Description)
    data object Clients : BottomNavItem(Screen.Clients.route, "Clients", Icons.Default.People)
    data object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Dashboard, Invoices, Clients, Settings)
    }
}
