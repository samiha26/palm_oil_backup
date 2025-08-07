package com.example.palm_oil

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.repository.TreeLocationRepository
import com.example.palm_oil.data.repository.ReconFormRepository
import kotlinx.coroutines.launch

class HarvesterDownloadMap : AppCompatActivity() {
    
    private lateinit var plotSpinner: Spinner
    private lateinit var downloadButton: Button
    private lateinit var treeLocationRepository: TreeLocationRepository
    private lateinit var reconFormRepository: ReconFormRepository
    
    private var availablePlots = mutableListOf<String>()
    private var selectedPlotId: String? = null
    
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
        loadAvailablePlots()
    }
    
    private fun initializeComponents() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        plotSpinner = findViewById(R.id.plotSpinner)
        downloadButton = findViewById<Button>(R.id.downloadButton)
        
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
                    downloadButton.isEnabled = true
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
    
    private fun loadAvailablePlots() {
        lifecycleScope.launch {
            try {
                // Get all plots that have been created by the recon team
                val plots = treeLocationRepository.getDistinctPlotIds()
                availablePlots.clear()
                availablePlots.addAll(plots)
                
                // Create spinner options with tree count information
                val plotOptions = mutableListOf("Select Plot to Download")
                
                for (plotId in plots) {
                    val treeCount = treeLocationRepository.getTreeLocationsCountByPlotId(plotId)
                    val reconForms = reconFormRepository.getReconFormsByPlotId(plotId)
                    plotOptions.add("$plotId ($treeCount trees)")
                }
                
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
                        "Found ${plots.size} plots available for download",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@HarvesterDownloadMap,
                    "Error loading available plots: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun downloadSelectedPlot() {
        selectedPlotId?.let { plotId ->
            lifecycleScope.launch {
                try {
                    // Get plot data
                    val trees = treeLocationRepository.getTreeLocationsByPlotId(plotId)
                    val reconForms = reconFormRepository.getReconFormsByPlotId(plotId)
                    
                    // Show download progress/success message
                    Toast.makeText(
                        this@HarvesterDownloadMap,
                        "Downloaded plot $plotId: ${trees.size} trees, ${reconForms.size} recon forms",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // In a real app, this would sync data from cloud
                    // For now, the data is already in the local database
                    
                    // You could add additional processing here, such as:
                    // - Marking plots as "downloaded for harvesting"
                    // - Creating local copies for offline use
                    // - Validating data integrity
                    
                    finish() // Return to harvester home
                    
                } catch (e: Exception) {
                    Toast.makeText(
                        this@HarvesterDownloadMap,
                        "Error downloading plot data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}