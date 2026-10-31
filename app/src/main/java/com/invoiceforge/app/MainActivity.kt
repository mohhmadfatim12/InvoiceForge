package com.invoiceforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.invoiceforge.app.data.local.preferences.ThemeMode
import com.invoiceforge.app.ui.navigation.InvoiceForgeNavGraph
import com.invoiceforge.app.ui.theme.InvoiceForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as InvoiceForgeApplication
        val container = app.container

        setContent {
            val themeMode by container.userPreferencesManager.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            InvoiceForgeTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    InvoiceForgeNavGraph(
                        navController = navController,
                        container = container
                    )
                }
            }
        }
    }
}
