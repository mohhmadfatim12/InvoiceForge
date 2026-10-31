package com.invoiceforge.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.invoiceforge.app.di.AppContainer
import com.invoiceforge.app.ui.clients.AddEditClientScreen
import com.invoiceforge.app.ui.clients.AddEditClientViewModel
import com.invoiceforge.app.ui.clients.ClientDetailScreen
import com.invoiceforge.app.ui.clients.ClientDetailViewModel
import com.invoiceforge.app.ui.clients.ClientListScreen
import com.invoiceforge.app.ui.clients.ClientListViewModel
import com.invoiceforge.app.ui.dashboard.DashboardScreen
import com.invoiceforge.app.ui.dashboard.DashboardViewModel
import com.invoiceforge.app.ui.invoices.AddEditInvoiceScreen
import com.invoiceforge.app.ui.invoices.AddEditInvoiceViewModel
import com.invoiceforge.app.ui.invoices.InvoiceDetailScreen
import com.invoiceforge.app.ui.invoices.InvoiceDetailViewModel
import com.invoiceforge.app.ui.invoices.InvoiceListScreen
import com.invoiceforge.app.ui.invoices.InvoiceListViewModel
import com.invoiceforge.app.ui.invoices.PdfPreviewScreen
import com.invoiceforge.app.ui.settings.SettingsScreen
import com.invoiceforge.app.ui.settings.SettingsViewModel

@Composable
fun InvoiceForgeNavGraph(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = BottomNavItem.items.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    BottomNavItem.items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(
                        container.invoiceRepository,
                        container.businessProfileRepository
                    )
                )
                DashboardScreen(
                    viewModel = vm,
                    onCreateInvoiceClick = { navController.navigate(Screen.AddEditInvoice.createRoute()) },
                    onInvoiceClick = { invoiceId -> navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId)) },
                    onViewAllInvoicesClick = { navController.navigate(Screen.Invoices.route) }
                )
            }

            // Invoices List
            composable(Screen.Invoices.route) {
                val vm: InvoiceListViewModel = viewModel(
                    factory = InvoiceListViewModel.Factory(
                        container.invoiceRepository,
                        container.businessProfileRepository
                    )
                )
                InvoiceListScreen(
                    viewModel = vm,
                    onCreateInvoiceClick = { navController.navigate(Screen.AddEditInvoice.createRoute()) },
                    onInvoiceClick = { invoiceId -> navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId)) }
                )
            }

            // Add/Edit Invoice
            composable(
                route = Screen.AddEditInvoice.route,
                arguments = listOf(
                    navArgument("invoiceId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStack ->
                val invoiceId = backStack.arguments?.getLong("invoiceId") ?: 0L
                val vm: AddEditInvoiceViewModel = viewModel(
                    factory = AddEditInvoiceViewModel.Factory(
                        invoiceId,
                        container.invoiceRepository,
                        container.clientRepository,
                        container.businessProfileRepository
                    )
                )
                AddEditInvoiceScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateClient = { navController.navigate(Screen.AddEditClient.createRoute()) },
                    onSaveSuccess = { savedId ->
                        navController.popBackStack()
                        navController.navigate(Screen.InvoiceDetail.createRoute(savedId))
                    }
                )
            }

            // Invoice Detail
            composable(
                route = Screen.InvoiceDetail.route,
                arguments = listOf(
                    navArgument("invoiceId") { type = NavType.LongType }
                )
            ) { backStack ->
                val invoiceId = backStack.arguments?.getLong("invoiceId") ?: 0L
                val vm: InvoiceDetailViewModel = viewModel(
                    factory = InvoiceDetailViewModel.Factory(
                        invoiceId,
                        container.invoiceRepository,
                        container.businessProfileRepository,
                        container.pdfInvoiceGenerator
                    )
                )
                InvoiceDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClick = { id -> navController.navigate(Screen.AddEditInvoice.createRoute(id)) },
                    onViewPdfClick = { id -> navController.navigate(Screen.PdfPreview.createRoute(id)) }
                )
            }

            // PDF Preview
            composable(
                route = Screen.PdfPreview.route,
                arguments = listOf(
                    navArgument("invoiceId") { type = NavType.LongType }
                )
            ) { backStack ->
                val invoiceId = backStack.arguments?.getLong("invoiceId") ?: 0L
                val vm: InvoiceDetailViewModel = viewModel(
                    factory = InvoiceDetailViewModel.Factory(
                        invoiceId,
                        container.invoiceRepository,
                        container.businessProfileRepository,
                        container.pdfInvoiceGenerator
                    )
                )
                PdfPreviewScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Clients List
            composable(Screen.Clients.route) {
                val vm: ClientListViewModel = viewModel(
                    factory = ClientListViewModel.Factory(
                        container.clientRepository,
                        container.invoiceRepository,
                        container.businessProfileRepository
                    )
                )
                ClientListScreen(
                    viewModel = vm,
                    onCreateClientClick = { navController.navigate(Screen.AddEditClient.createRoute()) },
                    onClientClick = { clientId -> navController.navigate(Screen.ClientDetail.createRoute(clientId)) }
                )
            }

            // Add/Edit Client
            composable(
                route = Screen.AddEditClient.route,
                arguments = listOf(
                    navArgument("clientId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStack ->
                val clientId = backStack.arguments?.getLong("clientId") ?: 0L
                val vm: AddEditClientViewModel = viewModel(
                    factory = AddEditClientViewModel.Factory(clientId, container.clientRepository)
                )
                AddEditClientScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { savedId ->
                        navController.popBackStack()
                        navController.navigate(Screen.ClientDetail.createRoute(savedId))
                    }
                )
            }

            // Client Detail
            composable(
                route = Screen.ClientDetail.route,
                arguments = listOf(
                    navArgument("clientId") { type = NavType.LongType }
                )
            ) { backStack ->
                val clientId = backStack.arguments?.getLong("clientId") ?: 0L
                val vm: ClientDetailViewModel = viewModel(
                    factory = ClientDetailViewModel.Factory(
                        clientId,
                        container.clientRepository,
                        container.invoiceRepository,
                        container.businessProfileRepository
                    )
                )
                ClientDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClientClick = { id -> navController.navigate(Screen.AddEditClient.createRoute(id)) },
                    onInvoiceClick = { invoiceId -> navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId)) },
                    onCreateInvoiceForClientClick = { _ ->
                        navController.navigate(Screen.AddEditInvoice.createRoute())
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.businessProfileRepository,
                        container.userPreferencesManager,
                        container.invoiceRepository,
                        container.clientRepository,
                        container.backupManager
                    )
                )
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
