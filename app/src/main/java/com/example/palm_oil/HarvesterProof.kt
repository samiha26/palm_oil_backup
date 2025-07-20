package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.ui.harvesterproof.HarvesterProofViewModel

class HarvesterProof : AppCompatActivity() {
    
    private lateinit var viewModel: HarvesterProofViewModel
    private lateinit var treeIdInput: EditText
    private lateinit var plotIdInput: EditText

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
        setupObservers()
    }

    private fun initializeViews() {
        treeIdInput = findViewById(R.id.editText)
        plotIdInput = findViewById(R.id.editPlotId)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val captureButton = findViewById<ImageButton>(R.id.captureButton)

        backButton.setOnClickListener {
            finish()
        }

        captureButton.setOnClickListener {
            val treeId = treeIdInput.text.toString().trim()
            val plotId = plotIdInput.text.toString().trim()
            
            if (treeId.isEmpty()) {
                Toast.makeText(this, "Please enter Tree ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (plotId.isEmpty()) {
                Toast.makeText(this, "Please enter Plot ID", Toast.LENGTH_SHORT).show()
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
        plotIdInput.text.clear()
        viewModel.clearForm()
    }
}