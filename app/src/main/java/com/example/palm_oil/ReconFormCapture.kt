package com.example.palm_oil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.palm_oil.utils.PlotManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.ui.viewmodel.ReconFormViewModel
import kotlinx.coroutines.launch

class ReconFormCapture : AppCompatActivity() {
    private lateinit var viewModel: ReconFormViewModel
    private lateinit var textTreeId: TextView
    private lateinit var spinnerPlotId: Spinner
    private lateinit var editNumberOfFruits: EditText
    private lateinit var radioGroupHarvestDays: RadioGroup
    private lateinit var saveBtn: Button
    private lateinit var uploadBtn: Button

    private var selectedPlotId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_form_capture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ReconFormViewModel::class.java]

        // Get tree ID from intent and set it in ViewModel
        val treeId = intent.getStringExtra("TREE_ID") ?: ""
        Log.d("ReconFormCapture", "Received tree ID from intent: '$treeId'")
        if (treeId.isNotEmpty()) {
            viewModel.setTreeId(treeId)
            Log.d("ReconFormCapture", "Set tree ID in ViewModel: '$treeId'")
        } else {
            Log.w("ReconFormCapture", "No tree ID received from intent")
        }

        // Initialize views
        initializeViews()

        // Fetch plots from API
        fetchPlots()

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel data
        observeViewModel()
    }

    private fun initializeViews() {
        textTreeId = findViewById(R.id.textTreeId)
        spinnerPlotId = findViewById(R.id.spinnerPlotId)
        editNumberOfFruits = findViewById(R.id.editNumberOfFruits)
        radioGroupHarvestDays = findViewById(R.id.radioGroupHarvestDays)
        saveBtn = findViewById(R.id.saveBtn)
    }

    private fun fetchPlots() {
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.apiService
                val response = apiService.getPlots(ApiClient.getApiKey())

                if (response.isSuccessful) {
                    val plotsResponse = response.body()
                    Log.d("ReconFormCapture", "Plots response: $plotsResponse")
                    plotsResponse?.let {
                        val apiPlotIds = it.plots.map { plot -> plot.id }
                        Log.d("ReconFormCapture", "Fetched ${apiPlotIds.size} plots from API: $apiPlotIds")
                        
                        // Use PlotManager to get plots (prioritizing API data)
                        val plotIds = PlotManager.getPlots(apiPlotIds, this@ReconFormCapture)
                        setupPlotSpinner(plotIds)
                    } ?: run {
                        Log.e("ReconFormCapture", "Response body is null")
                        // Use cached or default plots from PlotManager
                        val defaultPlots = PlotManager.getPlots(null, this@ReconFormCapture)
                        setupPlotSpinner(defaultPlots)
                        Toast.makeText(this@ReconFormCapture, "Using local plot list", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ReconFormCapture", "Failed to fetch plots: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("ReconFormCapture", "Error body: $errorBody")
                    // Use cached or default plots from PlotManager
                    val defaultPlots = PlotManager.getPlots(null, this@ReconFormCapture)
                    setupPlotSpinner(defaultPlots)
                    Toast.makeText(this@ReconFormCapture, "Using local plot list (API error: ${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ReconFormCapture", "Error fetching plots", e)
                // Use default plots from PlotManager
                val defaultPlots = com.example.palm_oil.utils.PlotManager.getPlots(null)
                setupPlotSpinner(defaultPlots)
                Toast.makeText(this@ReconFormCapture, "Using local plot list (${e.message})", Toast.LENGTH_SHORT).show()
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
                Log.d("ReconFormCapture", "Selected plot: $selectedPlotId (position: $position)")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPlotId = null
            }
        }
    }

    private fun setupClickListeners() {
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        saveBtn.setOnClickListener {
            saveForm()
        }
    }





    private fun observeViewModel() {
        viewModel.currentTreeId.observe(this) { treeId ->
            Log.d("ReconFormCapture", "Tree ID observed: $treeId")
            textTreeId.text = treeId
        }

        viewModel.saveStatus.observe(this) { success ->
            Log.d("ReconFormCapture", "Save status: $success")
            if (success) {
                Toast.makeText(this, "Form saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error saving form", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveForm() {
        val plotId = selectedPlotId
        val fruitsText = editNumberOfFruits.text.toString().trim()

        Log.d("ReconFormCapture", "Saving form - plotId: $plotId, fruitsText: $fruitsText")

        if (plotId.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a plot ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (fruitsText.isEmpty()) {
            Toast.makeText(this, "Please enter number of fruits", Toast.LENGTH_SHORT).show()
            return
        }
        
        val numberOfFruits = try {
            fruitsText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number of fruits", Toast.LENGTH_SHORT).show()
            return
        }
        
        val harvestDays = when (radioGroupHarvestDays.checkedRadioButtonId) {
            R.id.radio1Day -> 1
            R.id.radio2Days -> 2
            R.id.radio3Days -> 3
            else -> 1
        }
        
        Log.d("ReconFormCapture", "Form data - numberOfFruits: $numberOfFruits, harvestDays: $harvestDays")
        
        // Update ViewModel with form data
        viewModel.setPlotId(plotId)
        viewModel.setNumberOfFruits(numberOfFruits)
        viewModel.setHarvestDays(harvestDays)
        
        // Save the form
        viewModel.saveReconForm()
    }
}