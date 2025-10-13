package com.example.palm_oil.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utility class to cache plot IDs in local storage
 */
object PlotCacheManager {
    private const val TAG = "PlotCacheManager"
    private const val PREF_NAME = "plot_cache"
    private const val KEY_PLOTS = "cached_plots"
    private const val KEY_LAST_UPDATED = "last_updated_time"
    
    /**
     * Save a list of plot IDs to shared preferences
     */
    fun savePlots(context: Context, plots: List<String>) {
        try {
            val gson = Gson()
            val json = gson.toJson(plots)
            getPrefs(context).edit()
                .putString(KEY_PLOTS, json)
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Saved ${plots.size} plots to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save plots to cache", e)
        }
    }
    
    /**
     * Retrieve the cached plot IDs
     * @return List of plot IDs or null if cache is empty
     */
    fun getCachedPlots(context: Context): List<String>? {
        try {
            val json = getPrefs(context).getString(KEY_PLOTS, null) ?: return null
            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            val plots = gson.fromJson<List<String>>(json, type)
            Log.d(TAG, "Loaded ${plots.size} plots from cache")
            return plots
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve plots from cache", e)
            return null
        }
    }
    
    /**
     * Check if the cached plot list is still valid (not too old)
     * @param maxAgeMs Maximum age in milliseconds (default 7 days)
     */
    fun isCacheValid(context: Context, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000): Boolean {
        val lastUpdated = getPrefs(context).getLong(KEY_LAST_UPDATED, 0)
        if (lastUpdated == 0L) return false
        
        val age = System.currentTimeMillis() - lastUpdated
        return age < maxAgeMs
    }
    
    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}