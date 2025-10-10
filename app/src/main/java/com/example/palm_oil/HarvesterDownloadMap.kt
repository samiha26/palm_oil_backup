package com.example.palm_oil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.TreeLocationEntity
import com.example.palm_oil.data.repository.TreeLocationRepository
import com.example.palm_oil.data.repository.ReconFormRepository
import com.example.palm_oil.utils.NetworkUtils
import kotlinx.coroutines.launch

data class DownloadProgress(
    val isDownloading: Boolean = false,
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val currentOperation: String = "",
    val error: String? = null
)

class HarvesterDownloadMap : AppCompatActivity() {
    
    private lateinit var plotSpinner: Spinner
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var treeLocationRepository: TreeLocationRepository
    private lateinit var reconFormRepository: ReconFormRepository
    
    private var availablePlots = mutableListOf<String>()
    private var selectedPlotId: String? = null
    
    private val _downloadProgress = MutableLiveData<DownloadProgress>()
    private val downloadProgress: LiveData<DownloadProgress> = _downloadProgress
    
    companion object {
        private const val TAG = "HarvesterDownloadMap"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_download_map)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeComponents()
        setupRepositories()
        setupUI()
        observeDownloadProgress()
        checkNetworkAndLoadPlots()
    }
    
    private fun observeDownloadProgress() {
        downloadProgress.observe(this) { progress ->
            if (progress.isDownloading) {
                // Show progress indicators
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                downloadButton.isEnabled = false
                plotSpinner.isEnabled = false
                
                // Update progress
                progressBar.max = progress.totalItems
                progressBar.progress = progress.currentItem
                progressText.text = progress.currentOperation
            } else {
                // Hide progress indicators
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                downloadButton.isEnabled = selectedPlotId != null
                plotSpinner.isEnabled = true
                
                // Show error if any
                progress.error?.let { error ->
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun checkNetworkAndLoadPlots() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            loadAvailablePlots()
        } else {
            Toast.makeText(
                this,
                "No internet connection. You need an internet connection to download maps.",
                Toast.LENGTH_LONG
            ).show()
            downloadButton.isEnabled = false
        }
    }
    
    private fun initializeComponents() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        plotSpinner = findViewById(R.id.plotSpinner)
        downloadButton = findViewById<Button>(R.id.downloadButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        
        // Initially hide progress indicators
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRepositories() {
        val database = PalmOilDatabase.getDatabase(this)
        treeLocationRepository = TreeLocationRepository(database.treeLocationDao())
        reconFormRepository = ReconFormRepository(database.reconFormDao())
    }
    
    private fun setupUI() {
        // Plot spinner selection listener
        plotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Select Plot" option
                    selectedPlotId = availablePlots[position - 1]
                    downloadButton.isEnabled = NetworkUtils.isNetworkAvailable(this@HarvesterDownloadMap)
                } else {
                    selectedPlotId = null
                    downloadButton.isEnabled = false
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPlotId = null
                downloadButton.isEnabled = false
            }
        }
        
        // Download button click listener
        downloadButton.setOnClickListener {
            downloadSelectedPlot()
        }
        
        // Initially disable download button
        downloadButton.isEnabled = false
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check network connectivity when resuming the activity
        val isConnected = NetworkUtils.isNetworkAvailable(this)
        if (!isConnected) {
            Toast.makeText(
                this,
                "No internet connection. You need internet to download maps.",
                Toast.LENGTH_LONG
            ).show()
            downloadButton.isEnabled = false
        } else if (selectedPlotId != null) {
            downloadButton.isEnabled = true
        }
        
        // Update connection status text
        val connectionStatusText = findViewById<TextView>(R.id.connectionStatusText)
        connectionStatusText.text = if (isConnected) {
            "Connected to internet. Maps will be downloaded from cloud and stored locally for offline viewing."
        } else {
            "No internet connection. You need internet to download maps from cloud."
        }
    }
    
    private fun loadAvailablePlots() {
        _downloadProgress.postValue(
            DownloadProgress(
                isDownloading = true,
                currentOperation = "Fetching available plots from cloud..."
            )
        )
        
        lifecycleScope.launch {
            try {
                // Get plots from the cloud API
                val response = ApiClient.apiService.getPlots(
                    apiKey = ApiClient.getApiKey()
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val plotsResponse = response.body()!!
                    val plots = plotsResponse.plots.map { it.id }
                    
                    availablePlots.clear()
                    availablePlots.addAll(plots)
                    
                    // Create spinner options
                    val plotOptions = mutableListOf("Select Plot to Download")
                    plotOptions.addAll(plots.map { "$it" })
                    
                    // Set up spinner adapter
                    val adapter = ArrayAdapter(
                        this@HarvesterDownloadMap,
                        android.R.layout.simple_spinner_item,
                        plotOptions
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    plotSpinner.adapter = adapter
                    
                    if (plots.isEmpty()) {
                        Toast.makeText(
                            this@HarvesterDownloadMap,
                            "No plots available for download. Please ensure recon team has uploaded data.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@HarvesterDownloadMap,
                            "Found ${plots.size} plots available for download from cloud",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    _downloadProgress.postValue(DownloadProgress(isDownloading = false))
                    
                } else {
                    val errorMessage = "Failed to fetch plots: ${response.code()}"
                    Log.e(TAG, errorMessage)
                    Toast.makeText(
                        this@HarvesterDownloadMap,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    
                    _downloadProgress.postValue(
                        DownloadProgress(
                            isDownloading = false,
                            error = errorMessage
                        )
                    )
                }
                
            } catch (e: Exception) {
                val errorMessage = "Error loading plots from cloud: ${e.message}"
                Log.e(TAG, errorMessage, e)
                
                Toast.makeText(
                    this@HarvesterDownloadMap,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
                
                _downloadProgress.postValue(
                    DownloadProgress(
                        isDownloading = false,
                        error = errorMessage
                    )
                )
            }
        }
    }
    
    private fun downloadSelectedPlot() {
        selectedPlotId?.let { plotId ->
            lifecycleScope.launch {
                try {
                    // Check network connection first
                    if (!NetworkUtils.isNetworkAvailable(this@HarvesterDownloadMap)) {
                        Toast.makeText(
                            this@HarvesterDownloadMap,
                            "No internet connection available. Please connect to the internet and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    
                    _downloadProgress.postValue(
                        DownloadProgress(
                            isDownloading = true,
                            currentOperation = "Downloading tree locations for plot $plotId from cloud..."
                        )
                    )
                    
                    // Fetch tree locations from the cloud API
                    val treeResponse = ApiClient.apiService.getTreeLocationsByPlot(
                        apiKey = ApiClient.getApiKey(),
                        plotId = plotId
                    )
                    
                    if (treeResponse.isSuccessful && treeResponse.body() != null) {
                        val treeLocationsResponse = treeResponse.body()!!
                        val cloudTreeLocations = treeLocationsResponse.locations
                        
                        _downloadProgress.postValue(
                            DownloadProgress(
                                isDownloading = true,
                                currentOperation = "Downloading recon forms with harvest day data for plot $plotId..."
                            )
                        )
                        
                        // Also download recon forms for this plot to get harvest day information
                        val formResponse = ApiClient.apiService.getFormsByPlot(
                            apiKey = ApiClient.getApiKey(),
                            plotId = plotId
                        )
                        
                        val reconForms = if (formResponse.isSuccessful && formResponse.body() != null) {
                            formResponse.body()!!.forms
                        } else {
                            Log.w(TAG, "Failed to fetch recon forms: ${formResponse.code()}")
                            emptyList()
                        }
                        
                        _downloadProgress.postValue(
                            DownloadProgress(
                                isDownloading = true,
                                currentOperation = "Saving ${cloudTreeLocations.size} tree locations and ${reconForms.size} recon forms to local database..."
                            )
                        )
                        
                        // Clear any existing tree locations for this plot to avoid duplicates
                        val existingTreeLocations = treeLocationRepository.getTreeLocationsByPlotId(plotId)
                        for (existingLocation in existingTreeLocations) {
                            treeLocationRepository.deleteTreeLocation(existingLocation)
                        }
                        
                        // Clear any existing recon forms for this plot to avoid duplicates
                        val existingReconForms = reconFormRepository.getReconFormsByPlotId(plotId)
                        existingReconForms.forEach { form ->
                            reconFormRepository.deleteReconFormById(form.id)
                        }
                        
                        // Calculate the total number of items to save for progress tracking
                        val totalItems = cloudTreeLocations.size + reconForms.size
                        var currentItem = 0
                        
                        // Convert tree locations API response to local database entities and save them
                        cloudTreeLocations.forEach { cloudLocation ->
                            currentItem++
                            _downloadProgress.postValue(
                                DownloadProgress(
                                    isDownloading = true,
                                    currentItem = currentItem,
                                    totalItems = totalItems,
                                    currentOperation = "Saving data... (${currentItem}/${totalItems})"
                                )
                            )
                            
                            val treeLocation = TreeLocationEntity(
                                id = 0, // Auto-generated
                                treeId = cloudLocation.tree_id,
                                plotId = cloudLocation.plot_id,
                                xCoordinate = cloudLocation.x_coordinate.toFloat(),
                                yCoordinate = cloudLocation.y_coordinate.toFloat(),
                                latitude = cloudLocation.latitude,
                                longitude = cloudLocation.longitude,
                                createdAt = cloudLocation.created_at,
                                updatedAt = cloudLocation.updated_at,
                                notes = cloudLocation.notes,
                                isSynced = true, // Mark as synced since it came from the cloud
                                syncTimestamp = System.currentTimeMillis() // Set sync timestamp to now
                            )
                            
                            treeLocationRepository.insertTreeLocation(treeLocation)
                        }
                        
                        // Convert recon forms API response to local database entities and save them
                        reconForms.forEach { form ->
                            currentItem++
                            _downloadProgress.postValue(
                                DownloadProgress(
                                    isDownloading = true,
                                    currentItem = currentItem,
                                    totalItems = totalItems,
                                    currentOperation = "Saving harvest plan data... (${currentItem}/${totalItems})"
                                )
                            )
                            
                            val reconFormEntity = com.example.palm_oil.data.database.ReconFormEntity(
                                id = 0, // Auto-generated
                                treeId = form.tree_id,
                                plotId = form.plot_id ?: plotId, // Use the current plot ID if null in the form
                                numberOfFruits = form.number_of_fruits ?: 0,
                                harvestDays = form.harvest_days ?: 1, // Default to day 1 if not specified
                                createdAt = form.created_at ?: System.currentTimeMillis(),
                                isSynced = true // Mark as synced since it came from cloud
                            )
                            
                            reconFormRepository.insertReconForm(reconFormEntity)
                        }
                        
                        // Update download status in shared preferences
                        val sharedPrefs = getSharedPreferences("harvester_prefs", MODE_PRIVATE)
                        with(sharedPrefs.edit()) {
                            putLong("${plotId}_last_download", System.currentTimeMillis())
                            putInt("${plotId}_tree_count", cloudTreeLocations.size)
                            putInt("${plotId}_recon_form_count", reconForms.size)
                            apply()
                        }
                        
                        // Show success message with stats
                        _downloadProgress.postValue(DownloadProgress(isDownloading = false))
                        
                        val harvestDaysInfo = if (reconForms.isNotEmpty()) {
                            val harvestDayGroups = reconForms.groupBy { it.harvest_days }
                            val daysInfo = harvestDayGroups.map { (day, forms) -> 
                                "Day $day: ${forms.size} trees" 
                            }.joinToString(", ")
                            " ($daysInfo)"
                        } else {
                            ""
                        }
                        
                        Toast.makeText(
                            this@HarvesterDownloadMap,
                            "Successfully downloaded plot $plotId from cloud: ${cloudTreeLocations.size} trees and ${reconForms.size} harvest plans$harvestDaysInfo",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Return to harvester home
                        finish()
                        
                    } else {
                        val errorBody = treeResponse.errorBody()?.string() ?: "Unknown error"
                        val errorMessage = "Failed to download plot data: ${treeResponse.code()} - $errorBody"
                        Log.e(TAG, errorMessage)
                        
                        _downloadProgress.postValue(
                            DownloadProgress(
                                isDownloading = false,
                                error = "Failed to download plot data (${treeResponse.code()})"
                            )
                        )
                        
                        Toast.makeText(
                            this@HarvesterDownloadMap,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading plot data", e)
                    
                    _downloadProgress.postValue(
                        DownloadProgress(
                            isDownloading = false,
                            error = "Error: ${e.message}"
                        )
                    )
                    
                    Toast.makeText(
                        this@HarvesterDownloadMap,
                        "Error downloading plot data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}