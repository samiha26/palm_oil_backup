package com.example.palm_oil

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.repository.TreeLocationRepository
import com.example.palm_oil.ui.virtualmap.VirtualMapView
import com.example.palm_oil.ui.virtualmap.VirtualMapViewModel
import com.example.palm_oil.utils.LocationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VirtualMapActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VirtualMapActivity"
    }
    
    private lateinit var mapView: VirtualMapView
    private lateinit var plotSpinner: Spinner
    private lateinit var fabAddTreeAtLocation: FloatingActionButton
    private lateinit var viewModel: VirtualMapViewModel
    private lateinit var plotAdapter: ArrayAdapter<String>
    private lateinit var locationHelper: LocationHelper
    
    private var currentPlotId: String? = null
    private var currentLocation: Location? = null
    private var isTrackingLocation = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_virtual_map)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupActionBar()
        initializeViews()
        initializeViewModel()
        initializeLocationHelper()
        setupMapView()
        setupPlotSpinner()
        loadPlots()
        requestLocationPermissions()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Virtual Map"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        plotSpinner = findViewById(R.id.plotSpinner)
        fabAddTreeAtLocation = findViewById(R.id.fabAddTreeAtLocation)
        
        // Setup FAB click listener
        fabAddTreeAtLocation.setOnClickListener {
            addTreeAtCurrentLocation()
        }
    }
    
    private fun initializeViewModel() {
        val database = PalmOilDatabase.getDatabase(this)
        val repository = TreeLocationRepository(database.treeLocationDao())
        val factory = VirtualMapViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[VirtualMapViewModel::class.java]
    }
    
    private fun initializeLocationHelper() {
        locationHelper = LocationHelper(this)
        // Set up default plot bounds
        setDefaultPlotBounds()
    }
    
    private fun setupMapView() {
        mapView.setOnMapClickListener { x, y ->
            currentPlotId?.let { plotId ->
                // For manual tapping, use the tap coordinates (no GPS)
                showAddTreeDialog(plotId, x, y, null, null)
            } ?: run {
                Toast.makeText(this, "Please select a plot first", Toast.LENGTH_SHORT).show()
            }
        }
        
        mapView.setOnTreeClickListener { tree ->
            showTreeOptionsDialog(tree)
        }
        
        // Set initial plot bounds (this should be customized per plot)
        setDefaultPlotBounds()
    }
    
    private fun setupPlotSpinner() {
        plotAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        plotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        plotSpinner.adapter = plotAdapter
        
        plotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Select Plot" item
                    val selectedPlot = plotAdapter.getItem(position)
                    currentPlotId = selectedPlot
                    selectedPlot?.let { loadTreesForPlot(it) }
                } else {
                    currentPlotId = null
                    mapView.setTrees(emptyList())
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentPlotId = null
                mapView.setTrees(emptyList())
            }
        }
    }
    
    private fun loadPlots() {
        lifecycleScope.launch {
            val plots = viewModel.getDistinctPlotIds()
            plotAdapter.clear()
            plotAdapter.add("Select Plot")
            
            if (plots.isEmpty()) {
                // Add default plots if no plots exist
                plotAdapter.add("Plot A")
                plotAdapter.add("Plot B")
                plotAdapter.add("Plot C")
            } else {
                plotAdapter.addAll(plots)
            }
            
            plotAdapter.notifyDataSetChanged()
        }
    }
    
    private fun loadTreesForPlot(plotId: String) {
        viewModel.getTreeLocationsByPlotIdLiveData(plotId).observe(this) { trees ->
            mapView.setTrees(trees)
        }
    }
    
    private fun showAddTreeDialog(plotId: String, x: Float, y: Float, latitude: Double? = null, longitude: Double? = null) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Add New Tree")
        
        val message = if (latitude != null && longitude != null) {
            val bounds = getPlotBounds(plotId)
            val realWorldX = (longitude - bounds[1]) * 111000.0 * Math.cos(Math.toRadians(latitude))
            val realWorldY = (latitude - bounds[0]) * 111000.0
            
            "PLANTATION POSITION\n" +
            "GPS: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}\n" +
            "Field Position: ${realWorldX.toInt()}m E, ${realWorldY.toInt()}m N\n" +
            "Map Position: (${x.toInt()}, ${y.toInt()})\n\n" +
            "Standard spacing: 8.5m between trees\n" +
            "Walking time: ~30 seconds between trees"
        } else {
            "MANUAL PLACEMENT\n" +
            "Map Position: (${x.toInt()}, ${y.toInt()})\n" +
            "Real-world: ~${mapToRealWorldDistance(x, getPlotBounds(plotId)).toInt()}m E, " +
            "~${mapToRealWorldDistance(2000f - y, getPlotBounds(plotId)).toInt()}m N"
        }
        dialog.setMessage(message)
        
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter Tree ID (e.g., T001, A-15, Block1-Row5-Tree3)"
        input.filters = arrayOf(InputFilter.LengthFilter(30)) // Increased length for detailed IDs
        dialog.setView(input)
        
        dialog.setPositiveButton("Plant Tree") { _, _ ->
            val treeId = input.text.toString().trim()
            if (treeId.isNotEmpty()) {
                addTree(plotId, treeId, x, y, latitude, longitude)
            } else {
                Toast.makeText(this, "Please enter a valid Tree ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
    
    private fun showTreeOptionsDialog(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        val options = arrayOf("View Details", "Edit Tree ID", "Move Tree", "Delete Tree")
        
        AlertDialog.Builder(this)
            .setTitle("Tree: ${tree.treeId}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTreeDetails(tree)
                    1 -> showEditTreeDialog(tree)
                    2 -> enterMoveMode(tree)
                    3 -> showDeleteTreeDialog(tree)
                }
            }
            .show()
    }
    
    private fun showTreeDetails(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        lifecycleScope.launch {
            // Get nearby trees for distance calculation
            val allTrees = viewModel.getTreeLocationsByPlotId(tree.plotId)
            val nearbyTrees = allTrees.filter { it.id != tree.id }
                .map { otherTree ->
                    val distance = if (tree.latitude != null && tree.longitude != null && 
                                     otherTree.latitude != null && otherTree.longitude != null) {
                        calculateDistance(tree.latitude, tree.longitude, otherTree.latitude, otherTree.longitude)
                    } else {
                        // Calculate using map coordinates if GPS not available
                        val mapDistance = Math.sqrt(
                            Math.pow((tree.xCoordinate - otherTree.xCoordinate).toDouble(), 2.0) +
                            Math.pow((tree.yCoordinate - otherTree.yCoordinate).toDouble(), 2.0)
                        ).toFloat()
                        mapToRealWorldDistance(mapDistance, getPlotBounds(tree.plotId))
                    }
                    Pair(otherTree, distance)
                }
                .sortedBy { it.second }
                .take(3) // Show 3 nearest trees
            
            val realWorldPos = if (tree.latitude != null && tree.longitude != null) {
                val bounds = getPlotBounds(tree.plotId)
                val realWorldX = (tree.longitude - bounds[1]) * 111000.0 * Math.cos(Math.toRadians(tree.latitude))
                val realWorldY = (tree.latitude - bounds[0]) * 111000.0
                "${realWorldX.toInt()}m E, ${realWorldY.toInt()}m N"
            } else {
                "Map coordinates only"
            }
            
            val nearbyTreesText = if (nearbyTrees.isNotEmpty()) {
                "\nNearest Trees:\n" + nearbyTrees.joinToString("\n") { 
                    "• ${it.first.treeId}: ${String.format("%.1f", it.second)}m away"
                }
            } else {
                "\nNo other trees in this plot"
            }
            
            val details = """
                Tree ID: ${tree.treeId}
                Plot: ${tree.plotId}
                Field Position: $realWorldPos
                Planted: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(tree.createdAt)}
                ${if (tree.notes?.isNotEmpty() == true) "Notes: ${tree.notes}" else "No notes"}
                $nearbyTreesText
                
                Standard palm spacing: 8.5m
                Walk time to next tree: ~30s
            """.trimIndent()
            
            AlertDialog.Builder(this@VirtualMapActivity)
                .setTitle("Palm Tree Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Add Notes") { _, _ ->
                    showAddNotesDialog(tree)
                }
                .show()
        }
    }
    
    private fun showEditTreeDialog(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Edit Tree ID")
        
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(tree.treeId)
        input.selectAll()
        input.filters = arrayOf(InputFilter.LengthFilter(20))
        dialog.setView(input)
        
        dialog.setPositiveButton("Update") { _, _ ->
            val newTreeId = input.text.toString().trim()
            if (newTreeId.isNotEmpty() && newTreeId != tree.treeId) {
                updateTree(tree.copy(treeId = newTreeId, updatedAt = System.currentTimeMillis()))
            }
        }
        
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
    
    private fun showAddNotesDialog(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Add Notes")
        
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.setText(tree.notes ?: "")
        input.hint = "Enter notes about this tree"
        dialog.setView(input)
        
        dialog.setPositiveButton("Save") { _, _ ->
            val notes = input.text.toString().trim()
            lifecycleScope.launch {
                viewModel.updateTreeLocationNotes(tree.id, notes)
            }
        }
        
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
    
    private fun showDeleteTreeDialog(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Tree")
            .setMessage("Are you sure you want to delete tree ${tree.treeId}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteTreeLocation(tree)
                    Toast.makeText(this@VirtualMapActivity, "Tree deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun enterMoveMode(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        mapView.selectTree(tree.id)
        Toast.makeText(this, "Tap on map to move tree ${tree.treeId}", Toast.LENGTH_LONG).show()
        
        // Temporarily change map click behavior
        mapView.setOnMapClickListener { x, y ->
            lifecycleScope.launch {
                viewModel.moveTreeLocation(tree.id, x, y)
                Toast.makeText(this@VirtualMapActivity, "Tree moved", Toast.LENGTH_SHORT).show()
                mapView.selectTree(null)
                
                // Restore normal click behavior
                setupMapView()
            }
        }
    }
    
    private fun showCreatePlotDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Create New Plot")
        
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter Plot ID"
        input.filters = arrayOf(InputFilter.LengthFilter(20))
        dialog.setView(input)
        
        dialog.setPositiveButton("Create") { _, _ ->
            val plotId = input.text.toString().trim()
            if (plotId.isNotEmpty()) {
                // Add to spinner and select it
                plotAdapter.add(plotId)
                plotAdapter.notifyDataSetChanged()
                
                // Select the new plot
                val position = plotAdapter.getPosition(plotId)
                plotSpinner.setSelection(position)
                
                Toast.makeText(this, "Plot created: $plotId", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid Plot ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
    
    private fun addTree(plotId: String, treeId: String, x: Float, y: Float, latitude: Double? = null, longitude: Double? = null) {
        lifecycleScope.launch {
            try {
                if (viewModel.isTreeExistsInPlot(treeId, plotId)) {
                    Toast.makeText(this@VirtualMapActivity, "Tree $treeId already exists in this plot", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                viewModel.createTreeLocation(treeId, plotId, x, y, latitude, longitude)
                Toast.makeText(this@VirtualMapActivity, "Tree added: $treeId", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@VirtualMapActivity, "Error adding tree: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateTree(tree: com.example.palm_oil.data.database.TreeLocationEntity) {
        lifecycleScope.launch {
            viewModel.updateTreeLocation(tree)
            Toast.makeText(this@VirtualMapActivity, "Tree updated", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.virtual_map_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_create_plot -> {
                showCreatePlotDialog()
                true
            }
            R.id.action_center_map -> {
                // Center on user location if available, otherwise reset map view
                if (currentLocation != null) {
                    mapView.centerOnUserLocation()
                } else {
                    mapView.resetView()
                }
                true
            }
            R.id.action_plot_stats -> {
                showPlotStats()
                true
            }
            R.id.action_gps_debug -> {
                showGpsDebugInfo()
                true
            }
            R.id.action_plantation_guide -> {
                showPlantationGuide()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showPlotStats() {
        currentPlotId?.let { plotId ->
            lifecycleScope.launch {
                val treeCount = viewModel.getTreeLocationsCountByPlotId(plotId)
                val trees = viewModel.getTreeLocationsByPlotId(plotId)
                
                // Calculate plantation statistics
                val plotArea = 25.0 // hectares (500m x 500m)
                val standardTreesPerHectare = 140 // Malaysian standard
                val expectedTrees = (plotArea * standardTreesPerHectare).toInt()
                val completionPercentage = if (expectedTrees > 0) (treeCount * 100.0 / expectedTrees) else 0.0
                
                // Calculate average tree spacing if we have trees with GPS coordinates
                val treesWithGps = trees.filter { it.latitude != null && it.longitude != null }
                val averageSpacing = if (treesWithGps.size >= 2) {
                    var totalDistance = 0f
                    var count = 0
                    for (i in treesWithGps.indices) {
                        for (j in i + 1 until treesWithGps.size) {
                            val tree1 = treesWithGps[i]
                            val tree2 = treesWithGps[j]
                            val distance = calculateDistance(
                                tree1.latitude!!, tree1.longitude!!,
                                tree2.latitude!!, tree2.longitude!!
                            )
                            if (distance < 50f) { // Only consider nearby trees (within 50m)
                                totalDistance += distance
                                count++
                            }
                        }
                    }
                    if (count > 0) totalDistance / count else null
                } else null
                
                val locationStatus = if (currentLocation != null) {
                    "GPS: Active (±${currentLocation?.accuracy?.toInt()}m)"
                } else {
                    "GPS: Not available"
                }
                
                val spacingStatus = averageSpacing?.let { 
                    val status = when {
                        it < 7f -> "Too close"
                        it > 10f -> "Too far"
                        else -> "Good"
                    }
                    "\nAvg spacing: ${String.format("%.1f", it)}m ($status)"
                } ?: "\nSpacing: Need more GPS data"
                
                val message = """
                    PLANTATION STATISTICS
                    
                    Plot: $plotId
                    Trees planted: $treeCount
                    Target trees: $expectedTrees (${String.format("%.1f", completionPercentage)}%)
                    Plot area: ${plotArea}ha (500m × 500m)
                    $spacingStatus
                    $locationStatus
                    
                    Standard: 8.5m spacing, 140 trees/ha
                """.trimIndent()
                
                AlertDialog.Builder(this@VirtualMapActivity)
                    .setTitle("Plantation Statistics")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        } ?: run {
            Toast.makeText(this, "Please select a plot first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showGpsDebugInfo() {
        val debugInfo = StringBuilder()
        debugInfo.append("=== GPS DEBUG INFO ===\n\n")
        
        // Permission status
        debugInfo.append("Permissions:\n")
        debugInfo.append("- Fine location: ${locationHelper.hasLocationPermission()}\n")
        debugInfo.append("- Location enabled: ${locationHelper.isLocationEnabled()}\n\n")
        
        // Current location info
        currentLocation?.let { location ->
            debugInfo.append("CURRENT Location:\n")
            debugInfo.append("- Latitude: ${String.format("%.8f", location.latitude)}\n")
            debugInfo.append("- Longitude: ${String.format("%.8f", location.longitude)}\n")
            debugInfo.append("- Accuracy: ${location.accuracy}m\n")
            debugInfo.append("- Provider: ${location.provider}\n")
            debugInfo.append("- Age: ${(System.currentTimeMillis() - location.time) / 1000}s ago\n")
            debugInfo.append("- Altitude: ${location.altitude}m\n")
            debugInfo.append("- Speed: ${location.speed}m/s\n")
            debugInfo.append("- Bearing: ${location.bearing}°\n\n")
        } ?: run {
            debugInfo.append("CURRENT Location: Not available\n\n")
        }
        
        // Tracking status
        debugInfo.append("Status:\n")
        debugInfo.append("- Tracking active: $isTrackingLocation\n")
        debugInfo.append("- Selected plot: ${currentPlotId ?: "None"}\n\n")
        
        debugInfo.append("Tips for better GPS:\n")
        debugInfo.append("• Go outside for better signal\n")
        debugInfo.append("• Move at least 3-5 meters to see changes\n")
        debugInfo.append("• GPS accuracy is typically 3-5 meters\n")
        debugInfo.append("• Check logcat for detailed location updates")
        
        AlertDialog.Builder(this)
            .setTitle("GPS Debug Information")
            .setMessage(debugInfo.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy to Clipboard") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("GPS Debug", debugInfo.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    // Location permission handling
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Precise location access granted
                startLocationTracking()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Approximate location access granted
                startLocationTracking()
            }
            else -> {
                // No location access granted
                showLocationPermissionExplanation()
            }
        }
    }
    
    private fun requestLocationPermissions() {
        when {
            locationHelper.hasLocationPermission() -> {
                startLocationTracking()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionExplanation()
            }
            else -> {
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }
    
    private fun showLocationPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location access to show your position on the map and automatically place trees at your current location.")
            .setPositiveButton("Grant Permission") { _, _ ->
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            .setNegativeButton("Skip") { _, _ ->
                Toast.makeText(this, "You can still use the map by tapping to place trees manually", Toast.LENGTH_LONG).show()
            }
            .show()
    }
    
    private fun startLocationTracking() {
        if (!locationHelper.hasLocationPermission()) return
        
        if (!locationHelper.isLocationEnabled()) {
            showLocationSettingsDialog()
            return
        }
        
        Log.d(TAG, "Starting GPS location tracking...")
        
        // Get last known location immediately
        lifecycleScope.launch {
            locationHelper.getLastKnownLocation()?.let { location ->
                Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                updateLocation(location)
            } ?: Log.d(TAG, "No last known location available")
        }
        
        // Start continuous location updates
        lifecycleScope.launch {
            locationHelper.getLocationUpdates()
                .catch { e ->
                    Log.e(TAG, "Location update error: ${e.message}", e)
                    Toast.makeText(this@VirtualMapActivity, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .collect { location ->
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                    updateLocation(location)
                }
        }
        
        isTrackingLocation = true
        Toast.makeText(this, "GPS tracking started - check logs for location updates", Toast.LENGTH_LONG).show()
    }
    
    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS Disabled")
            .setMessage("Please enable GPS/Location services to use location-based features.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateLocation(location: Location) {
        val previousLocation = currentLocation
        currentLocation = location
        mapView.setUserLocation(location.latitude, location.longitude, location.accuracy)
        
        // Log location changes for debugging
        if (previousLocation != null) {
            val distance = previousLocation.distanceTo(location)
            Log.d(TAG, "Location changed - Distance moved: ${distance}m, " +
                    "from (${previousLocation.latitude}, ${previousLocation.longitude}) " +
                    "to (${location.latitude}, ${location.longitude})")
        } else {
            Log.d(TAG, "First location update: ${location.latitude}, ${location.longitude}")
        }
        
        // Update FAB color based on GPS accuracy
        updateFabBasedOnGpsAccuracy(location.accuracy)
    }
    
    private fun addTreeAtCurrentLocation() {
        val location = currentLocation
        if (location == null) {
            Toast.makeText(this, "GPS location not available. Please wait for GPS fix or tap on map manually.", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Add tree at current location requested but no GPS location available")
            return
        }
        
        val plotId = currentPlotId
        if (plotId == null) {
            Toast.makeText(this, "Please select a plot first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convert GPS coordinates to map coordinates using current location
        val plotBounds = getPlotBounds(plotId)
        val mapCoords = gpsToMapCoordinates(location.latitude, location.longitude, plotBounds)
        
        Log.d(TAG, "Adding tree at CURRENT GPS location: ${location.latitude}, ${location.longitude} " +
                "-> Map coords: ${mapCoords.first}, ${mapCoords.second} (accuracy: ${location.accuracy}m)")
        
        showAddTreeDialog(plotId, mapCoords.first, mapCoords.second, location.latitude, location.longitude)
    }
    
    private fun updateFabBasedOnGpsAccuracy(accuracy: Float) {
        val color = when {
            accuracy <= 5f -> android.R.color.holo_green_dark // Very accurate
            accuracy <= 10f -> android.R.color.holo_orange_dark // Good
            accuracy <= 20f -> android.R.color.holo_red_dark // Poor
            else -> android.R.color.darker_gray // Very poor
        }
        fabAddTreeAtLocation.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, color)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
        Log.d(TAG, "Activity destroyed, location updates stopped")
    }
    
    private fun showPlantationGuide() {
        val guideInfo = """
            MALAYSIAN PALM OIL PLANTATION GUIDE
            
            STANDARD SPECIFICATIONS:
            • Tree spacing: 8.5m × 8.5m (triangular pattern)
            • Trees per hectare: 140 trees
            • Plot area: 25 hectares (500m × 500m)
            • Row spacing: 8.5 meters
            
            FIELD WORK:
            • Walking time between trees: ~30 seconds
            • Daily survey capacity: ~200-300 trees
            • GPS accuracy needed: ±3-5 meters
            
            MAP SCALE:
            • Virtual map: 2000×2000 pixels
            • Real world: 500m × 500m
            • 1 pixel = 0.25 meters
            
            USAGE TIPS:
            • Use GPS button when standing near tree
            • Tap map for precise manual positioning
            • Maintain 8.5m spacing between trees
            • Check plot stats for completion progress
            
            TREE NAMING:
            • Use systematic IDs: Row-Tree (e.g., R1-T15)
            • Or Block-Row-Tree (e.g., A-5-23)
            • Include plot reference in notes
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Plantation Field Guide")
            .setMessage(guideInfo)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun setDefaultPlotBounds() {
        // Malaysian Palm Oil Plantation realistic bounds
        // Based on your GPS location: 3.2125186, 101.6730024
        // Creating a realistic plantation plot of approximately 25 hectares (500m x 500m)
        
        val centerLat = 3.2125186   // Your actual GPS latitude
        val centerLng = 101.6730024 // Your actual GPS longitude
        
        // Convert 500m to degrees (approximately)
        // 1 degree latitude ≈ 111,000 meters
        // 1 degree longitude ≈ 111,000 * cos(latitude) meters
        val latOffset = 250.0 / 111000.0  // 250m in each direction = 500m total
        val lngOffset = 250.0 / (111000.0 * Math.cos(Math.toRadians(centerLat))) // Adjust for longitude at this latitude
        
        val minLat = centerLat - latOffset  // ~3.210
        val maxLat = centerLat + latOffset  // ~3.215
        val minLng = centerLng - lngOffset  // ~101.670
        val maxLng = centerLng + lngOffset  // ~101.676
        
        Log.d(TAG, "Setting plantation bounds:")
        Log.d(TAG, "  Center: ($centerLat, $centerLng)")
        Log.d(TAG, "  Bounds: lat[$minLat, $maxLat] lng[$minLng, $maxLng]")
        Log.d(TAG, "  Coverage: ~500m x 500m (~25 hectares)")
        
        mapView.setPlotBounds(minLat, minLng, maxLat, maxLng)
    }
    
    private fun getPlotBounds(plotId: String): FloatArray {
        // Return realistic plantation bounds for each plot
        // All plots use the same bounds for now, but you can customize per plot later
        val centerLat = 3.2125186
        val centerLng = 101.6730024
        val latOffset = 250.0 / 111000.0
        val lngOffset = 250.0 / (111000.0 * Math.cos(Math.toRadians(centerLat)))
        
        return floatArrayOf(
            (centerLat - latOffset).toFloat(),  // minLat
            (centerLng - lngOffset).toFloat(),  // minLng
            (centerLat + latOffset).toFloat(),  // maxLat
            (centerLng + lngOffset).toFloat()   // maxLng
        )
    }
    
    private fun gpsToMapCoordinates(latitude: Double, longitude: Double, bounds: FloatArray): Pair<Float, Float> {
        val x = ((longitude - bounds[1]) / (bounds[3] - bounds[1]) * 2000f).toFloat()
        val y = 2000f - ((latitude - bounds[0]) / (bounds[2] - bounds[0]) * 2000f).toFloat()
        
        // Calculate real-world position within the plantation
        val realWorldX = (longitude - bounds[1]) * 111000.0 * Math.cos(Math.toRadians(latitude))  // meters from western edge
        val realWorldY = (latitude - bounds[0]) * 111000.0  // meters from southern edge
        
        // Debug logging for coordinate conversion
        Log.d(TAG, "GPS to Map conversion:")
        Log.d(TAG, "  GPS: ($latitude, $longitude)")
        Log.d(TAG, "  Bounds: lat[${bounds[0]}, ${bounds[2]}] lng[${bounds[1]}, ${bounds[3]}]")
        Log.d(TAG, "  Real-world position: ${realWorldX.toInt()}m E, ${realWorldY.toInt()}m N from plot origin")
        Log.d(TAG, "  Map coords: ($x, $y)")
        Log.d(TAG, "  Clamped coords: (${x.coerceIn(0f, 2000f)}, ${y.coerceIn(0f, 2000f)})")
        
        return Pair(x.coerceIn(0f, 2000f), y.coerceIn(0f, 2000f))
    }
    
    // Helper function to calculate real-world distance between two GPS points
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    // Helper function to convert map coordinates back to real-world meters
    private fun mapToRealWorldDistance(mapDistance: Float, bounds: FloatArray): Float {
        // Map is 2000x2000 pixels representing 500m x 500m real world
        return (mapDistance / 2000f) * 500f  // Convert to meters
    }
}
