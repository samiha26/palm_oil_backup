package com.example.palm_oil

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.repository.TreeLocationRepository
import com.example.palm_oil.ui.virtualmap.VirtualMapViewModel
import com.example.palm_oil.utils.MapUtils
import kotlinx.coroutines.launch

class MapSummaryActivity : AppCompatActivity() {
    
    private lateinit var viewModel: VirtualMapViewModel
    
    private lateinit var totalTreesText: TextView
    private lateinit var totalPlotsText: TextView
    private lateinit var unsyncedTreesText: TextView
    private lateinit var plotsRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map_summary)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupActionBar()
        initializeViews()
        initializeViewModel()
        loadSummaryData()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Map Summary"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initializeViews() {
        totalTreesText = findViewById(R.id.totalTreesText)
        totalPlotsText = findViewById(R.id.totalPlotsText)
        unsyncedTreesText = findViewById(R.id.unsyncedTreesText)
        plotsRecyclerView = findViewById(R.id.plotsRecyclerView)
        
        plotsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun initializeViewModel() {
        val database = PalmOilDatabase.getDatabase(this)
        val repository = TreeLocationRepository(database.treeLocationDao())
        val factory = VirtualMapViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[VirtualMapViewModel::class.java]
    }
    
    private fun loadSummaryData() {
        lifecycleScope.launch {
            // Load basic statistics
            val totalTrees = viewModel.getTreeLocationsCount()
            val unsyncedTrees = viewModel.getUnsyncedTreeLocationsCount()
            val plots = viewModel.getDistinctPlotIds()
            
            totalTreesText.text = "Total Trees: $totalTrees"
            totalPlotsText.text = "Total Plots: ${plots.size}"
            unsyncedTreesText.text = "Unsynced Trees: $unsyncedTrees"
            
            // Load detailed plot statistics
            val plotStats = mutableListOf<PlotStatistics>()
            for (plotId in plots) {
                val plotTrees = viewModel.getTreeLocationsByPlotId(plotId)
                val plotStats1 = PlotStatistics(
                    plotId = plotId,
                    treeCount = plotTrees.size,
                    density = if (plotTrees.isNotEmpty()) {
                        MapUtils.calculateTreeDensity(
                            plotTrees.map { it.toTreeLocation() },
                            0f, 0f, 2000f, 2000f
                        )
                    } else 0f,
                    avgTreeSpacing = if (plotTrees.size > 1) {
                        calculateAverageTreeSpacing(plotTrees.map { it.toTreeLocation() })
                    } else 0f
                )
                plotStats.add(plotStats1)
            }
            
            // Setup RecyclerView adapter with plot statistics
            // plotsRecyclerView.adapter = PlotStatsAdapter(plotStats)
        }
    }
    
    private fun calculateAverageTreeSpacing(trees: List<com.example.palm_oil.data.model.TreeLocation>): Float {
        if (trees.size < 2) return 0f
        
        var totalDistance = 0f
        var pairCount = 0
        
        for (i in trees.indices) {
            for (j in i + 1 until trees.size) {
                totalDistance += MapUtils.calculateDistance(trees[i], trees[j])
                pairCount++
            }
        }
        
        return if (pairCount > 0) totalDistance / pairCount else 0f
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    data class PlotStatistics(
        val plotId: String,
        val treeCount: Int,
        val density: Float,
        val avgTreeSpacing: Float
    )
}

// Extension function to convert TreeLocationEntity to TreeLocation
private fun com.example.palm_oil.data.database.TreeLocationEntity.toTreeLocation(): com.example.palm_oil.data.model.TreeLocation {
    return com.example.palm_oil.data.model.TreeLocation(
        id = id,
        treeId = treeId,
        plotId = plotId,
        xCoordinate = xCoordinate,
        yCoordinate = yCoordinate,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced,
        syncTimestamp = syncTimestamp,
        notes = notes
    )
}
