package com.invoiceforge.app.data.repository

import com.invoiceforge.app.data.local.dao.ClientDao
import com.invoiceforge.app.data.local.dao.InvoiceDao
import com.invoiceforge.app.data.local.entity.ClientEntity
import com.invoiceforge.app.domain.model.ClientSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class ClientRepository(
    private val clientDao: ClientDao,
    private val invoiceDao: InvoiceDao
) {
    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()

    fun getClient(id: Long): Flow<ClientEntity?> = clientDao.getClientById(id)

    suspend fun getClientSync(id: Long): ClientEntity? = clientDao.getClientByIdSync(id)

    fun searchClients(query: String): Flow<List<ClientEntity>> = clientDao.searchClients(query)

    suspend fun saveClient(client: ClientEntity): Long {
        return if (client.id == 0L) {
            clientDao.insertClient(client)
        } else {
            clientDao.updateClient(client)
            client.id
        }
    }

    suspend fun deleteClient(client: ClientEntity) {
        clientDao.deleteClient(client)
    }

    suspend fun countInvoicesForClient(clientId: Long): Int {
        return clientDao.countInvoicesForClient(clientId)
    }

    fun getClientSummary(clientId: Long): Flow<ClientSummary?> {
        return combine(
            clientDao.getClientById(clientId),
            invoiceDao.getInvoicesForClient(clientId)
        ) { client, invoices ->
            if (client == null) return@combine null

            var totalInvoiced = BigDecimal.ZERO
            var totalPaid = BigDecimal.ZERO

            invoices.forEach { invoiceWithDetails ->
                val calc = invoiceWithDetails.calculate()
                totalInvoiced = totalInvoiced.add(calc.grandTotal)
                totalPaid = totalPaid.add(calc.amountPaid)
            }

            val outstanding = totalInvoiced.subtract(totalPaid).max(BigDecimal.ZERO)

            ClientSummary(
                clientId = client.id,
                clientName = client.name,
                company = client.company,
                totalInvoices = invoices.size,
                totalInvoiced = totalInvoiced,
                totalPaid = totalPaid,
                outstandingAmount = outstanding
            )
        }
    }
}
