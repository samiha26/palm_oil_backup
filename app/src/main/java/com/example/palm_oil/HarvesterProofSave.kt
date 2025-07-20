package com.example.palm_oil

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.ui.harvesterproof.HarvesterProofViewModel

class HarvesterProofSave : AppCompatActivity() {
    
    private lateinit var viewModel: HarvesterProofViewModel
    private lateinit var treeIdDisplay: TextView
    private lateinit var plotIdDisplay: TextView
    private var imagePath: String? = null
    private var treeId: String? = null
    private var plotId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_proof_save)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HarvesterProofViewModel::class.java]

        // Get data from intent
        imagePath = intent.getStringExtra("imagePath")
        treeId = intent.getStringExtra("tree_id")
        plotId = intent.getStringExtra("plot_id")

        Log.d("HarvesterProofSave", "Received imagePath: $imagePath, treeId: $treeId, plotId: $plotId")

        initializeViews()
        setupObservers()
        displayImage()
        setupFormData()
    }

    private fun initializeViews() {
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        treeIdDisplay = findViewById(R.id.editText)
        plotIdDisplay = findViewById(R.id.editPlotId)

        // Display the passed data
        treeIdDisplay.text = treeId ?: ""
        plotIdDisplay.text = plotId ?: ""

        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveHarvesterProof()
        }
    }

    private fun setupObservers() {
        // Observe save success
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Harvester proof saved successfully!", Toast.LENGTH_SHORT).show()
                
                // Navigate back to HarvesterProof activity
                val intent = Intent(this, HarvesterProof::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
        }

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

    private fun displayImage() {
        val imageView = findViewById<ImageView>(R.id.imageView)
        
        if (!imagePath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d("HarvesterProofSave", "Image displayed successfully")
                } else {
                    Log.w("HarvesterProofSave", "Failed to decode image from path: $imagePath")
                    imageView.setImageResource(android.R.color.darker_gray)
                }
            } catch (e: Exception) {
                Log.e("HarvesterProofSave", "Error loading image", e)
                imageView.setImageResource(android.R.color.darker_gray)
            }
        } else {
            Log.w("HarvesterProofSave", "No image path provided")
            imageView.setImageResource(android.R.color.darker_gray)
        }
    }

    private fun setupFormData() {
        // Set the data in ViewModel
        treeId?.let { 
            viewModel.setTreeId(it) 
            Log.d("HarvesterProofSave", "Set tree ID in ViewModel: $it")
        }
        
        plotId?.let { 
            viewModel.setPlotId(it)
            Log.d("HarvesterProofSave", "Set plot ID in ViewModel: $it")
        }
        
        imagePath?.let { 
            viewModel.setImagePath(it)
            Log.d("HarvesterProofSave", "Set image path in ViewModel: $it")
        }
    }

    private fun saveHarvesterProof() {
        Log.d("HarvesterProofSave", "Save button clicked")
        
        // For now, we don't have notes input in the layout, so we'll save without notes
        viewModel.setNotes("")

        // Validate required data
        if (treeId.isNullOrBlank()) {
            Toast.makeText(this, "Tree ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        if (plotId.isNullOrBlank()) {
            Toast.makeText(this, "Plot ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        if (imagePath.isNullOrBlank()) {
            Toast.makeText(this, "Image is missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the harvester proof
        viewModel.saveHarvesterProof()
    }
}