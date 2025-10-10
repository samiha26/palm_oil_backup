package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.ui.harvesterproof.HarvesterProofViewModel
import com.example.palm_oil.utils.NetworkUtils
import kotlinx.coroutines.launch

class HarvesterProof : AppCompatActivity() {

    private lateinit var viewModel: HarvesterProofViewModel
    private lateinit var treeIdInput: EditText
    private lateinit var spinnerPlotId: Spinner
    private var selectedPlotId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_proof)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HarvesterProofViewModel::class.java]

        // Initialize views
        initializeViews()

        // Fetch plots from API
        fetchPlots()

        setupObservers()
    }

    private fun initializeViews() {
        treeIdInput = findViewById(R.id.editText)
        spinnerPlotId = findViewById(R.id.spinnerPlotId)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val captureButton = findViewById<ImageButton>(R.id.captureButton)

        backButton.setOnClickListener {
            finish()
        }

        captureButton.setOnClickListener {
            val treeId = treeIdInput.text.toString().trim()
            val plotId = selectedPlotId

            if (treeId.isEmpty()) {
                Toast.makeText(this, "Please enter Tree ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (plotId.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a Plot ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Set data in ViewModel
            viewModel.setTreeId(treeId)
            viewModel.setPlotId(plotId)

            Log.d("HarvesterProof", "Starting camera with tree ID: $treeId, plot ID: $plotId")

            // Pass data to camera activity
            val intent = Intent(this, HarvesterProofCam::class.java).apply {
                putExtra("tree_id", treeId)
                putExtra("plot_id", plotId)
            }
            startActivity(intent)
        }
    }

    private fun fetchPlots() {
        lifecycleScope.launch {
            try {
                // Check network availability
                if (NetworkUtils.isNetworkAvailable(this@HarvesterProof)) {
                    // If online, try to fetch plots from API
                    try {
                        val apiService = ApiClient.apiService
                        val response = apiService.getPlots(ApiClient.getApiKey())

                        if (response.isSuccessful) {
                            val plotsResponse = response.body()
                            Log.d("HarvesterProof", "Plots response: $plotsResponse")
                            plotsResponse?.let {
                                val plotIds = it.plots.map { plot -> plot.id }
                                Log.d("HarvesterProof", "Fetched ${plotIds.size} plots from cloud: $plotIds")
                                setupPlotSpinner(plotIds)
                                
                                // Show online status
                                Toast.makeText(this@HarvesterProof, "Online mode: Showing plots from cloud", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HarvesterProof", "Error fetching plots from cloud: ${e.message}", e)
                        // If API call fails, we'll fall back to local database
                    }
                }
                
                // If offline or API call failed, fetch locally stored plots
                try {
                    val database = PalmOilDatabase.getDatabase(this@HarvesterProof)
                    val localPlotIds = database.treeLocationDao().getDistinctPlotIds()
                    
                    if (localPlotIds.isNotEmpty()) {
                        Log.d("HarvesterProof", "Fetched ${localPlotIds.size} plots from local database: $localPlotIds")
                        
                        setupPlotSpinner(localPlotIds)
                        
                        // Show offline status
                        Toast.makeText(this@HarvesterProof, "Offline mode: Showing downloaded plots from local storage", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("HarvesterProof", "No plots found in local database")
                        setupPlotSpinner(emptyList())
                        Toast.makeText(this@HarvesterProof, "No downloaded plots found. Please connect to internet and download plots first.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("HarvesterProof", "Error fetching plots from local database: ${e.message}", e)
                    Toast.makeText(this@HarvesterProof, "Error loading local plots: ${e.message}", Toast.LENGTH_SHORT).show()
                    setupPlotSpinner(emptyList())
                }
            } catch (e: Exception) {
                Log.e("HarvesterProof", "Unexpected error loading plots", e)
                Toast.makeText(this@HarvesterProof, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                setupPlotSpinner(emptyList())
            }
        }
    }

    private fun setupPlotSpinner(plotIds: List<String>) {
        // Add a prompt item at the beginning
        val items = if (plotIds.isNotEmpty()) {
            listOf("Select Plot") + plotIds
        } else {
            listOf("No plots available")
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlotId.adapter = adapter

        spinnerPlotId.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Position 0 is the prompt "Select Plot", so actual plots start at position 1
                selectedPlotId = if (position > 0 && plotIds.isNotEmpty()) {
                    plotIds.getOrNull(position - 1)
                } else {
                    null
                }
                Log.d("HarvesterProof", "Selected plot: $selectedPlotId (position: $position)")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPlotId = null
            }
        }
    }

    private fun setupObservers() {
        // Observe validation errors
        viewModel.validationError.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe general errors
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear the input fields when returning to this screen
        // so user can enter new data
        treeIdInput.text.clear()
        selectedPlotId = null
        // Reset spinner selection
        if (spinnerPlotId.adapter != null && spinnerPlotId.adapter.count > 0) {
            spinnerPlotId.setSelection(0)
        }
        viewModel.clearForm()
    }
}