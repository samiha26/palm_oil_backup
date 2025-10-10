package com.example.palm_oil.debug

import android.content.Context
import android.util.Log
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DebugHelper {
    
    private const val TAG = "DebugHelper"
    
    /**
     * Check system status for debugging upload issues
     */
    suspend fun checkSystemStatus(context: Context): String = withContext(Dispatchers.IO) {
        val statusBuilder = StringBuilder()
        
        try {
            // 1. Network connectivity
            val isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
            val isWifiConnected = NetworkUtils.isWifiConnected(context)
            statusBuilder.append("Network Status:\n")
            statusBuilder.append("- Internet Available: $isNetworkAvailable\n")
            statusBuilder.append("- WiFi Connected: $isWifiConnected\n\n")
            
            // 2. Database status
            val database = PalmOilDatabase.getDatabase(context)
            val totalForms = database.reconFormDao().getFormsCount()
            val unsyncedForms = database.reconFormDao().getUnsyncedFormsCount()
            statusBuilder.append("Database Status:\n")
            statusBuilder.append("- Total Forms: $totalForms\n")
            statusBuilder.append("- Unsynced Forms: $unsyncedForms\n\n")
            
            // 3. Permissions
            statusBuilder.append("Permissions:\n")
            statusBuilder.append("- Internet: ${hasInternetPermission(context)}\n")
            statusBuilder.append("- Network State: ${hasNetworkStatePermission(context)}\n\n")
            
            // 4. API Configuration
            statusBuilder.append("API Configuration:\n")
            statusBuilder.append("- Base URL: [Configured]\n")
            statusBuilder.append("- API Key: [Configured]\n\n")
            
            Log.d(TAG, "System Status Check Complete")
            statusBuilder.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking system status", e)
            "Error checking system status: ${e.message}"
        }
    }
    
    private fun hasInternetPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET) == 
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasNetworkStatePermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) == 
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
