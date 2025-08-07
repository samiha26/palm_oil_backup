package com.example.palm_oil

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.database.TreeLocationEntity
import com.example.palm_oil.data.model.TreeLocation
import com.example.palm_oil.data.repository.ReconFormRepository
import com.example.palm_oil.data.repository.TreeLocationRepository
import com.example.palm_oil.ui.harvestermap.HarvesterMapView
import com.example.palm_oil.utils.LocationHelper
import com.example.palm_oil.utils.MapUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class HarvesterVirtualMapView : AppCompatActivity() {
    
    companion object {
        private const val TAG = "HarvesterVirtualMapView"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var plotSpinner: Spinner
    private lateinit var mapCanvasContainer: FrameLayout
    private lateinit var pathwayText: TextView
    private lateinit var currentLocationButton: Button
    private lateinit var day1Button: Button
    private lateinit var day2Button: Button
    private lateinit var day3Button: Button
    
    // Map and Data
    private lateinit var harvesterMapView: HarvesterMapView
    private lateinit var treeLocationRepository: TreeLocationRepository
    private lateinit var reconFormRepository: ReconFormRepository
    private lateinit var locationHelper: LocationHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    // State
    private var availablePlots = mutableListOf<String>()
    private var currentPlotId: String? = null
    private var selectedHarvestDays = mutableSetOf<Int>() // Can select multiple days
    private var showCurrentLocation = true // Current location is always selected by default
    private var currentLocation: Location? = null
    private var allTrees = listOf<TreeLocationEntity>()
    private var allReconForms = listOf<ReconFormEntity>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_virtual_map_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeComponents()
        setupRepositories()
        setupUI()
        requestLocationPermission()
        loadAvailablePlots()
    }
    
    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        plotSpinner = findViewById(R.id.plotIdText)
        mapCanvasContainer = findViewById(R.id.mapCanvasContainer)
        pathwayText = findViewById(R.id.pathwayText)
        currentLocationButton = findViewById(R.id.button3D5)
        day1Button = findViewById(R.id.button3D3)
        day2Button = findViewById(R.id.button3D)
        day3Button = findViewById(R.id.button3D6)
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationHelper = LocationHelper(this)
        
        // Create and add the harvester map view
        harvesterMapView = HarvesterMapView(this)
        mapCanvasContainer.addView(harvesterMapView)
    }
    
    private fun setupRepositories() {
        val database = PalmOilDatabase.getDatabase(this)
        treeLocationRepository = TreeLocationRepository(database.treeLocationDao())
        reconFormRepository = ReconFormRepository(database.reconFormDao())
    }
    
    private fun setupUI() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Plot spinner
        plotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Select Plot" option
                    currentPlotId = availablePlots[position - 1]
                    loadPlotData()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Harvest day buttons
        currentLocationButton.setOnClickListener { toggleCurrentLocation() }
        day1Button.setOnClickListener { toggleHarvestDay(1) }
        day2Button.setOnClickListener { toggleHarvestDay(2) }
        day3Button.setOnClickListener { toggleHarvestDay(3) }
        
        // Set current location as default selected
        showCurrentLocation = true
        updateButtonStates()
    }
    
    private fun requestLocationPermission() {
        if (!locationHelper.hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location
            harvesterMapView.setUserLocation(location)
            if (showCurrentLocation) {
                updateMapView()
            }
        }
    }
    
    // Helper function to normalize plot IDs for consistent matching
    private fun normalizeePlotId(plotId: String): String {
        return plotId.trim()
            .lowercase()
            .replace("\\s+".toRegex(), " ") // Normalize whitespace
            .replace("plot\\s*".toRegex(), "") // Remove "plot" prefix
            .trim() // Remove any leading/trailing spaces after plot removal
    }
    
    // Helper function to find recon forms with flexible plot ID matching
    private suspend fun getReconFormsByFlexiblePlotId(targetPlotId: String): List<ReconFormEntity> {
        val allReconForms = reconFormRepository.getAllReconFormsSync()
        val normalizedTarget = normalizeePlotId(targetPlotId)
        
        return allReconForms.filter { form ->
            normalizeePlotId(form.plotId) == normalizedTarget
        }
    }
    
    // Temporary method to delete A1 recon form created today
    private fun deleteA1ReconFormToday() {
        lifecycleScope.launch {
            try {
                val allReconForms = reconFormRepository.getAllReconFormsSync()
                val today = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                
                // Find A1 recon forms created today
                val a1FormsToday = allReconForms.filter { form ->
                    form.treeId == "A1" && (today - form.createdAt) < oneDayMs
                }
                
                android.util.Log.d("HarvesterDebug", "Found ${a1FormsToday.size} A1 recon forms created today")
                a1FormsToday.forEach { form ->
                    android.util.Log.d("HarvesterDebug", "A1 Form: ID=${form.id}, TreeID=${form.treeId}, PlotID='${form.plotId}', Created=${java.util.Date(form.createdAt)}")
                }
                
                if (a1FormsToday.isNotEmpty()) {
                    // Delete all A1 forms created today
                    a1FormsToday.forEach { form ->
                        reconFormRepository.deleteReconForm(form)
                        android.util.Log.d("HarvesterDebug", "DELETED A1 recon form: ID=${form.id}, TreeID=${form.treeId}")
                    }
                    
                    Toast.makeText(
                        this@HarvesterVirtualMapView,
                        "Deleted ${a1FormsToday.size} A1 recon form(s) created today",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Reload the plots after deletion
                    loadAvailablePlots()
                } else {
                    Toast.makeText(
                        this@HarvesterVirtualMapView,
                        "No A1 recon forms created today found",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HarvesterDebug", "Error deleting A1 recon form: ${e.message}", e)
                Toast.makeText(this@HarvesterVirtualMapView, "Error deleting A1 recon form: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAvailablePlots() {
        lifecycleScope.launch {
            try {
                // Get all trees and recon forms to find matching plot IDs
                val totalTrees = treeLocationRepository.getAllTreeLocationsSync()
                val totalReconForms = reconFormRepository.getAllReconFormsSync()
                
                // Debug: Check total database contents
                android.util.Log.d("HarvesterDebug", "Total trees in database: ${totalTrees.size}")
                android.util.Log.d("HarvesterDebug", "Total recon forms in database: ${totalReconForms.size}")
                
                // Get all unique plot IDs from both trees and recon forms
                val treePlotIds = totalTrees.map { normalizeePlotId(it.plotId) }.distinct()
                val reconPlotIds = totalReconForms.map { normalizeePlotId(it.plotId) }.distinct()
                
                android.util.Log.d("HarvesterDebug", "Normalized tree plot IDs: ${treePlotIds.joinToString(", ")}")
                android.util.Log.d("HarvesterDebug", "Normalized recon form plot IDs: ${reconPlotIds.joinToString(", ")}")
                
                // Find plot IDs that have both trees and recon forms
                val matchingPlotIds = treePlotIds.intersect(reconPlotIds.toSet()).toList()
                android.util.Log.d("HarvesterDebug", "Matching plot IDs (have both trees and recon forms): ${matchingPlotIds.joinToString(", ")}")
                
                // Use original plot IDs from trees for display, but only include plots that have recon forms
                val availablePlotIds = totalTrees.map { it.plotId }.distinct()
                    .filter { originalPlotId -> 
                        matchingPlotIds.contains(normalizeePlotId(originalPlotId)) 
                    }
                
                availablePlots.clear()
                availablePlots.addAll(availablePlotIds)
                
                val plotOptions = mutableListOf("Select Plot")
                plotOptions.addAll(availablePlotIds)
                
                val adapter = ArrayAdapter(
                    this@HarvesterVirtualMapView,
                    android.R.layout.simple_spinner_item,
                    plotOptions
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                plotSpinner.adapter = adapter
                
                if (availablePlotIds.isEmpty()) {
                    Toast.makeText(
                        this@HarvesterVirtualMapView, 
                        "No plots with complete data found. Trees: ${totalTrees.size}, Forms: ${totalReconForms.size}. Please ensure recon forms are completed for tree locations.", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@HarvesterVirtualMapView, 
                        "Found ${availablePlotIds.size} plots with harvest data. Trees: ${totalTrees.size}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@HarvesterVirtualMapView, "Error loading plots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadPlotData() {
        currentPlotId?.let { plotId ->
            lifecycleScope.launch {
                try {
                    // Load trees for the selected plot (exact match)
                    allTrees = treeLocationRepository.getTreeLocationsByPlotId(plotId)
                    
                    // Load recon forms using flexible matching
                    allReconForms = getReconFormsByFlexiblePlotId(plotId)
                    
                    // Debug: Print matching analysis
                    val treeIds = allTrees.map { it.treeId }
                    val reconTreeIds = allReconForms.map { it.treeId }
                    
                    android.util.Log.d("HarvesterDebug", "=== PLOT DATA LOADING FOR: '$plotId' ===")
                    android.util.Log.d("HarvesterDebug", "Trees found: ${allTrees.size} - IDs: ${treeIds.joinToString(", ")}")
                    android.util.Log.d("HarvesterDebug", "Recon forms found: ${allReconForms.size} - Tree IDs: ${reconTreeIds.joinToString(", ")}")
                    
                    // Show which recon forms were matched
                    val matchedReconForms = allReconForms.map { "TreeID=${it.treeId}, PlotID='${it.plotId}', HarvestDay=${it.harvestDays}" }
                    android.util.Log.d("HarvesterDebug", "Matched recon forms: ${matchedReconForms.joinToString(" | ")}")
                    
                    // Debug: Check for matches/mismatches
                    val treesWithReconForms = treeIds.filter { reconTreeIds.contains(it) }
                    val treesWithoutReconForms = treeIds.filter { !reconTreeIds.contains(it) }
                    val reconFormsWithoutTrees = reconTreeIds.filter { !treeIds.contains(it) }
                    
                    android.util.Log.d("HarvesterDebug", "Trees with recon forms (${treesWithReconForms.size}): ${treesWithReconForms.joinToString(", ")}")
                    android.util.Log.d("HarvesterDebug", "Trees without recon forms (${treesWithoutReconForms.size}): ${treesWithoutReconForms.joinToString(", ")}")
                    android.util.Log.d("HarvesterDebug", "Recon forms without matching trees (${reconFormsWithoutTrees.size}): ${reconFormsWithoutTrees.joinToString(", ")}")
                    
                    if (allTrees.isEmpty()) {
                        Toast.makeText(
                            this@HarvesterVirtualMapView,
                            "No tree data found for plot $plotId. Please ensure recon team has uploaded data.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    
                    if (allReconForms.isEmpty()) {
                        Toast.makeText(
                            this@HarvesterVirtualMapView,
                            "No harvest planning data found for plot $plotId. Showing all trees without harvest day filtering.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Debug: Show harvest day distribution
                        val harvestDayGroups = allReconForms.groupBy { it.harvestDays }
                        val harvestDaysInfo = harvestDayGroups.map { (day, forms) -> 
                            "Day $day: ${forms.size} trees (${forms.map { it.treeId }.joinToString(",")})" 
                        }.joinToString(" | ")
                        
                        android.util.Log.d("HarvesterDebug", "Harvest day distribution: $harvestDaysInfo")
                        
                        Toast.makeText(
                            this@HarvesterVirtualMapView, 
                            "Loaded: ${allTrees.size} trees, ${allReconForms.size} harvest records. Match rate: ${treesWithReconForms.size}/${allTrees.size} trees", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    updateMapView()
                    
                } catch (e: Exception) {
                    android.util.Log.e("HarvesterDebug", "Error loading plot data: ${e.message}", e)
                    Toast.makeText(this@HarvesterVirtualMapView, "Error loading plot data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun toggleCurrentLocation() {
        showCurrentLocation = !showCurrentLocation
        updateButtonStates()
        updateMapView()
    }
    
    private fun toggleHarvestDay(day: Int) {
        if (selectedHarvestDays.contains(day)) {
            selectedHarvestDays.remove(day)
        } else {
            selectedHarvestDays.add(day)
        }
        updateButtonStates()
        updateMapView()
    }
    
    private fun updateButtonStates() {
        // Reset all buttons to default state
        val buttons = listOf(currentLocationButton, day1Button, day2Button, day3Button)
        buttons.forEach { button ->
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
        
        // Highlight current location button if selected
        if (showCurrentLocation) {
            currentLocationButton.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
            currentLocationButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        
        // Highlight selected harvest day buttons
        if (selectedHarvestDays.contains(1)) {
            day1Button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
            day1Button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        if (selectedHarvestDays.contains(2)) {
            day2Button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
            day2Button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        if (selectedHarvestDays.contains(3)) {
            day3Button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
            day3Button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }
    
    private fun updateMapView() {
        if (currentPlotId == null) return
        
        val treesToDisplay = getTreesForDisplay()
        
        // Always show the trees
        harvesterMapView.setTreesToHarvest(treesToDisplay)
        
        // Show path only when both current location and at least one harvest day are selected
        if (showCurrentLocation && selectedHarvestDays.isNotEmpty()) {
            val harvestPath = calculateOptimalPath(treesToDisplay)
            harvesterMapView.setHarvestPath(harvestPath)
            updatePathwayText(harvestPath)
        } else {
            // Clear existing path when conditions are not met
            harvesterMapView.setHarvestPath(emptyList())
            updatePathwayText(emptyList())
        }
        
        // Show current location if selected
        if (showCurrentLocation && currentLocation != null) {
            harvesterMapView.setUserLocation(currentLocation)
        }
    }
    
    private fun getTreesForDisplay(): List<TreeLocationEntity> {
        // If only current location is selected, show all trees
        if (showCurrentLocation && selectedHarvestDays.isEmpty()) {
            return allTrees
        }
        
        // Otherwise, show trees for selected harvest days
        val treesToShow = mutableListOf<TreeLocationEntity>()
        
        if (allReconForms.isNotEmpty() && selectedHarvestDays.isNotEmpty()) {
            for (day in selectedHarvestDays) {
                val treeIdsForDay = allReconForms
                    .filter { it.harvestDays == day }
                    .map { it.treeId }
                    .toSet()
                
                val treesForDay = allTrees.filter { tree ->
                    treeIdsForDay.contains(tree.treeId)
                }
                
                treesToShow.addAll(treesForDay)
            }
        }
        
        return treesToShow.distinctBy { it.id }
    }
    
    private fun calculateOptimalPath(trees: List<TreeLocationEntity>): List<TreeLocationEntity> {
        if (trees.isEmpty()) return emptyList()
        
        val startLocation = currentLocation
        val startX = startLocation?.let { 
            // Convert GPS to map coordinates if available
            // For now, use center of map as start point
            1000f
        } ?: 1000f
        val startY = startLocation?.let { 1000f } ?: 1000f
        
        // Convert TreeLocationEntity to TreeLocation for MapUtils
        val treeLocations = trees.map { it.toTreeLocation() }
        val optimizedPath = MapUtils.calculateOptimalPath(startX, startY, treeLocations)
        
        // Convert back to TreeLocationEntity
        return optimizedPath.map { treeLocation ->
            trees.first { it.id == treeLocation.id }
        }
    }
    
    private fun updatePathwayText(path: List<TreeLocationEntity>) {
        if (showCurrentLocation && selectedHarvestDays.isEmpty()) {
            // When only showing current location
            val locationStatus = if (currentLocation != null) "Current location shown" else "Waiting for location..."
            pathwayText.text = "$locationStatus\nShowing all ${allTrees.size} trees in plot${currentPlotId?.let { " $it" } ?: ""}"
            return
        }

        if (path.isEmpty()) {
            if (allTrees.isEmpty()) {
                pathwayText.text = "No tree data available. Please ensure recon team has uploaded data."
            } else if (selectedHarvestDays.isEmpty()) {
                pathwayText.text = "Select harvest days to display trees and path"
            } else if (allReconForms.isEmpty()) {
                pathwayText.text = "No harvest planning data available. Harvest day filtering is disabled."
            } else {
                pathwayText.text = "No trees scheduled for selected harvest days"
            }
            return
        }
        
        // Show path information when showing harvest days
        val pathString = path.joinToString(" → ") { it.treeId }
        val treeLocations = path.map { it.toTreeLocation() }
        val totalDistance = MapUtils.calculatePathDistance(treeLocations)
        
        val harvestInfo = if (selectedHarvestDays.isNotEmpty()) {
            "Harvest days: ${selectedHarvestDays.sorted().joinToString(",")}"
        } else {
            "No harvest days selected"
        }
        
        val infoText = "$harvestInfo\n" +
                      "Optimal path: $pathString\n" +
                      "Total distance: ${String.format("%.1f", totalDistance)}m"
        pathwayText.text = infoText
    }
    
    // Extension function to convert TreeLocationEntity to TreeLocation
    private fun TreeLocationEntity.toTreeLocation(): TreeLocation {
        return TreeLocation(
            id = this.id,
            treeId = this.treeId,
            plotId = this.plotId,
            xCoordinate = this.xCoordinate,
            yCoordinate = this.yCoordinate,
            latitude = this.latitude,
            longitude = this.longitude,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isSynced = this.isSynced,
            syncTimestamp = this.syncTimestamp,
            notes = this.notes
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission required for optimal navigation", Toast.LENGTH_LONG).show()
            }
        }
    }
}