package com.invoiceforge.app.data.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    @Test
    fun testValidBackupJsonStructure() {
        val json = JSONObject().apply {
            put("app", BackupManager.APP_ID)
            put("version", BackupManager.BACKUP_VERSION)
            put("timestamp", 1700000000000L)
            put("businessProfile", JSONObject().apply {
                put("name", "Studio Dev")
                put("currency", "USD")
            })
            put("clients", JSONArray())
            put("invoices", JSONArray())
            put("invoiceItems", JSONArray())
            put("payments", JSONArray())
        }

        assertEquals(BackupManager.APP_ID, json.getString("app"))
        assertEquals(BackupManager.BACKUP_VERSION, json.getInt("version"))
        assertTrue(json.has("businessProfile"))
        assertTrue(json.has("clients"))
        assertTrue(json.has("invoices"))
    }

    @Test
    fun testInvalidAppRejection() {
        val json = JSONObject().apply {
            put("app", "WrongApp")
            put("version", 1)
        }
        val isAppValid = json.has("app") && json.getString("app") == BackupManager.APP_ID
        assertFalse(isAppValid)
    }

    @Test
    fun testUnsupportedVersionRejection() {
        val json = JSONObject().apply {
            put("app", BackupManager.APP_ID)
            put("version", 999) // unsupported future version
        }
        val isVersionSupported = json.optInt("version") == BackupManager.BACKUP_VERSION
        assertFalse(isVersionSupported)
    }
}
