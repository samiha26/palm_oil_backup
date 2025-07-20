package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.adapter.GalleryGridAdapter
import com.example.palm_oil.data.model.GalleryImage
import com.example.palm_oil.ui.viewmodel.GalleryViewModel

class ReconGallery : AppCompatActivity() {
    private lateinit var viewModel: GalleryViewModel
    private lateinit var galleryGrid: GridView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var adapter: GalleryGridAdapter
    
    private var galleryImages: List<GalleryImage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_gallery)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GalleryViewModel::class.java]

        // Initialize views
        initializeViews()
        
        // Setup adapter
        setupGridAdapter()
        
        // Observe ViewModel
        observeViewModel()
        
        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        galleryGrid = findViewById(R.id.galleryGrid)
        progressBar = findViewById(R.id.progressBar)
        emptyText = findViewById(R.id.emptyText)
        
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupGridAdapter() {
        adapter = GalleryGridAdapter(this, galleryImages)
        galleryGrid.adapter = adapter
        
        adapter.setOnImageClickListener { position ->
            openFullScreenViewer(position)
        }
    }

    private fun observeViewModel() {
        viewModel.galleryImages.observe(this) { images ->
            galleryImages = images
            adapter.updateImages(images)
            
            if (images.isEmpty()) {
                galleryGrid.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
            } else {
                galleryGrid.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.refreshButton)?.setOnClickListener {
            viewModel.refreshGallery()
        }
    }

    private fun openFullScreenViewer(position: Int) {
        if (galleryImages.isEmpty()) return

        val intent = Intent(this, FullScreenImageViewer::class.java).apply {
            putExtra("CURRENT_POSITION", position)
            
            // Prepare data for intent
            val imagePaths = ArrayList(galleryImages.map { it.imagePath })
            val treeIds = ArrayList(galleryImages.map { it.treeId })
            val plotIds = ArrayList(galleryImages.map { it.plotId })
            val createdAts = galleryImages.map { it.createdAt }.toLongArray()
            val formIds = galleryImages.map { it.formId }.toLongArray()
            val imageIndexes = galleryImages.map { it.imageIndex }.toIntArray()
            
            putStringArrayListExtra("IMAGE_PATHS", imagePaths)
            putStringArrayListExtra("TREE_IDS", treeIds)
            putStringArrayListExtra("PLOT_IDS", plotIds)
            putExtra("CREATED_ATS", createdAts)
            putExtra("FORM_IDS", formIds)
            putExtra("IMAGE_INDEXES", imageIndexes)
        }
        
        startActivity(intent)
    }
}