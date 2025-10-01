package com.example.palm_oil.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.palm_oil.data.database.PalmOilDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SyncStatus(
    val unsyncedFormsCount: Int = 0,
    val localImagesCount: Int = 0,
    val hasDataToSync: Boolean = false,
    val lastSyncTimestamp: Long? = null
)

class SyncStatusManager(private val context: Context) {
    
    private val database = PalmOilDatabase.getDatabase(context)
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    /**
     * Check current sync status asynchronously
     */
    fun checkSyncStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Count unsynced forms
                val unsyncedFormsCount = database.reconFormDao().getUnsyncedFormsCount()
                
                // Count local images in app's image directory
                val localImagesCount = countLocalImages()
                
                // Get last sync timestamp from shared preferences
                val sharedPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                val lastSyncTimestamp = sharedPrefs.getLong("last_sync_timestamp", 0L).let {
                    if (it == 0L) null else it
                }
                
                val status = SyncStatus(
                    unsyncedFormsCount = unsyncedFormsCount,
                    localImagesCount = localImagesCount,
                    hasDataToSync = unsyncedFormsCount > 0 || localImagesCount > 0,
                    lastSyncTimestamp = lastSyncTimestamp
                )
                
                withContext(Dispatchers.Main) {
                    _syncStatus.value = status
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _syncStatus.value = SyncStatus()
                }
            }
        }
    }
    
    /**
     * Count images in the app's image directory
     */
    private fun countLocalImages(): Int {
        return try {
            val imageDir = File(context.getExternalFilesDir("images") ?: context.filesDir, "images")
            if (imageDir.exists() && imageDir.isDirectory) {
                imageDir.listFiles()?.count { file ->
                    file.isFile && (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp"))
                } ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Update last sync timestamp
     */
    fun updateLastSyncTimestamp() {
        val sharedPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("last_sync_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Mark all forms as synced
     */
    suspend fun markAllFormsAsSynced() {
        withContext(Dispatchers.IO) {
            database.reconFormDao().markAllFormsAsSynced()
        }
    }
}
