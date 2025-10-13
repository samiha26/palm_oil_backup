package com.example.palm_oil.utils

import android.content.Context
import android.util.Log

/**
 * Utility class for managing plot IDs, both from local fixed list and remote API
 */
object PlotManager {
    private const val TAG = "PlotManager"
    
    // Default fixed plot list from A to Z that can be used offline
    private val DEFAULT_PLOTS = ('A'..'Z').map { it.toString() }
    
    /**
     * Get all available plot IDs following this priority:
     * 1. API plots if available
     * 2. Cached plots if available and valid
     * 3. Default fixed plots (A-Z)
     *
     * @param apiPlots List of plot IDs fetched from API, or null if API call failed/unavailable
     * @param context Context to access cache (optional, only needed when apiPlots is null)
     * @return List of plot IDs to display in UI
     */
    fun getPlots(apiPlots: List<String>? = null, context: Context? = null): List<String> {
        // First priority: API plots
        if (!apiPlots.isNullOrEmpty()) {
            Log.d(TAG, "Using ${apiPlots.size} plots from API")
            // If context is provided, cache the plots for later
            if (context != null) {
                PlotCacheManager.savePlots(context, apiPlots)
            }
            return apiPlots
        }
        
        // Second priority: Cached plots (if context is provided)
        if (context != null) {
            if (PlotCacheManager.isCacheValid(context)) {
                val cachedPlots = PlotCacheManager.getCachedPlots(context)
                if (!cachedPlots.isNullOrEmpty()) {
                    Log.d(TAG, "Using ${cachedPlots.size} plots from cache")
                    return cachedPlots
                }
            }
        }
        
        // Last resort: Default fixed plots
        Log.d(TAG, "Using ${DEFAULT_PLOTS.size} default plots (A-Z)")
        return DEFAULT_PLOTS
    }
    
    /**
     * Check if a plot ID is valid (either in API list, cache, or default list)
     *
     * @param plotId Plot ID to validate
     * @param apiPlots List of plot IDs from API, or null if using cached/default list
     * @param context Context to access cache (optional)
     * @return True if plotId is valid
     */
    fun isValidPlot(plotId: String, apiPlots: List<String>? = null, context: Context? = null): Boolean {
        return getPlots(apiPlots, context).contains(plotId)
    }
}