package com.example.palm_oil

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.ui.viewmodel.ReconFormViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ReconFormCapture : AppCompatActivity() {
    private lateinit var viewModel: ReconFormViewModel
    private lateinit var textTreeId: TextView
    private lateinit var editPlotId: EditText
    private lateinit var editNumberOfFruits: EditText
    private lateinit var radioGroupHarvestDays: RadioGroup
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var imageView3: ImageView
    private lateinit var saveBtn: Button
    
    private val imageViews = mutableListOf<ImageView>()
    private var currentImageIndex = 0
    
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_form_capture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imagePath = result.data?.getStringExtra("imagePath")
                if (imagePath != null) {
                    viewModel.addImage(imagePath)
                }
            }
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
        
        // Set up image views list
        imageViews.addAll(listOf(imageView1, imageView2, imageView3))
        
        // Set up click listeners
        setupClickListeners()
        
        // Observe ViewModel data
        observeViewModel()
    }

    private fun initializeViews() {
        textTreeId = findViewById(R.id.textTreeId)
        editPlotId = findViewById(R.id.editPlotId)
        editNumberOfFruits = findViewById(R.id.editNumberOfFruits)
        radioGroupHarvestDays = findViewById(R.id.radioGroupHarvestDays)
        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)
        imageView3 = findViewById(R.id.imageView3)
        saveBtn = findViewById(R.id.saveBtn)
    }

    private fun setupClickListeners() {
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        val captureButton = findViewById<android.widget.ImageButton>(R.id.captureButton)

        backButton.setOnClickListener {
            finish()
        }
        
        captureButton.setOnClickListener {
            if (currentImageIndex < 3) {
                val intent = Intent(this, ReconCamera::class.java)
                cameraLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
            }
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
        
        viewModel.currentImages.observe(this) { images ->
            updateImageViews(images)
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

    private fun updateImageViews(images: List<String>) {
        // Hide all image views first
        imageViews.forEach { it.visibility = ImageView.GONE }
        
        // Show and load images
        images.forEachIndexed { index, imagePath ->
            if (index < imageViews.size) {
                val imageView = imageViews[index]
                imageView.visibility = ImageView.VISIBLE
                loadImageFromPath(imagePath, imageView)
            }
        }
        
        currentImageIndex = images.size
    }

    private fun loadImageFromPath(imagePath: String, imageView: ImageView) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveForm() {
        val plotId = editPlotId.text.toString().trim()
        val fruitsText = editNumberOfFruits.text.toString().trim()
        
        Log.d("ReconFormCapture", "Saving form - plotId: $plotId, fruitsText: $fruitsText")
        
        if (plotId.isEmpty()) {
            Toast.makeText(this, "Please enter plot ID", Toast.LENGTH_SHORT).show()
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