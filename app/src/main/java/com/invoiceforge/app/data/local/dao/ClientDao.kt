package com.invoiceforge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.invoiceforge.app.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsSync(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientById(id: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientByIdSync(id: Long): ClientEntity?

    @Query("""
        SELECT * FROM clients 
        WHERE name LIKE '%' || :query || '%' 
           OR company LIKE '%' || :query || '%' 
           OR email LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity): Int

    @Delete
    suspend fun deleteClient(client: ClientEntity): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE clientId = :clientId")
    suspend fun countInvoicesForClient(clientId: Long): Int

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}
