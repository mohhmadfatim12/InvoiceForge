package com.invoiceforge.app.data.repository

import com.invoiceforge.app.data.local.dao.BusinessProfileDao
import com.invoiceforge.app.data.local.entity.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(
    private val dao: BusinessProfileDao
) {
    val profile: Flow<BusinessProfileEntity?> = dao.getProfile()

    suspend fun getProfileSync(): BusinessProfileEntity? {
        return dao.getProfileSync()
    }

    suspend fun saveProfile(profile: BusinessProfileEntity): Long {
        return dao.insertOrUpdate(profile.copy(id = 1L))
    }
}
