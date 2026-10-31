package com.invoiceforge.app

import android.app.Application
import com.invoiceforge.app.di.AppContainer
import com.invoiceforge.app.di.DefaultAppContainer

class InvoiceForgeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
