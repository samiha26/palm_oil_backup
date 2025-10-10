package com.example.palm_oil.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class NetworkUtils {
    companion object {
        /**
         * Check if device has internet connection
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                
                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                activeNetworkInfo?.isConnectedOrConnecting == true
            }
        }
        
        /**
         * Check if device can actually reach the internet (not just connected to network)
         */
        suspend fun canReachInternet(): Boolean {
            return try {
                val runtime = Runtime.getRuntime()
                val process = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
                val exitValue = process.waitFor()
                exitValue == 0
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if device has wifi connection specifically
         */
        fun isWifiConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected
            }
        }
    }
}
