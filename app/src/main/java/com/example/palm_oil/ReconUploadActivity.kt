package com.example.palm_oil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.service.UploadService
import com.example.palm_oil.utils.NetworkUtils
import com.example.palm_oil.utils.SyncStatusManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReconUploadActivity : AppCompatActivity() {
    
    private lateinit var networkIcon: ImageView
    private lateinit var networkStatusText: TextView
    private lateinit var unsyncedFormsCount: TextView
    private lateinit var buttonUploadForms: Button
    private lateinit var buttonUploadImages: Button
    private lateinit var buttonTestConnection: Button
    private lateinit var progressLayout: LinearLayout
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var lastSyncText: TextView
    
    private lateinit var syncStatusManager: SyncStatusManager
    private lateinit var uploadService: UploadService
    
    // Gallery picker for multiple images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uploadImages(uris)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_upload)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeViews()
        initializeServices()
        setupClickListeners()
        observeData()
        
        // Initial status check
        checkNetworkStatus()
        syncStatusManager.checkSyncStatus()
    }
    
    private fun initializeViews() {
        networkIcon = findViewById(R.id.networkIcon)
        networkStatusText = findViewById(R.id.networkStatusText)
        unsyncedFormsCount = findViewById(R.id.unsyncedFormsCount)
        buttonUploadForms = findViewById(R.id.buttonUploadForms)
        buttonUploadImages = findViewById(R.id.buttonUploadImages)
        buttonTestConnection = findViewById(R.id.buttonTestConnection)
        progressLayout = findViewById(R.id.progressLayout)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        progressText = findViewById(R.id.progressText)
        lastSyncText = findViewById(R.id.lastSyncText)
    }
    
    private fun initializeServices() {
        syncStatusManager = SyncStatusManager(this)
        uploadService = UploadService(this)
    }
    
    private fun setupClickListeners() {
        // Back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Upload forms button
        buttonUploadForms.setOnClickListener {
            uploadForms()
        }
        
        // Upload images button
        buttonUploadImages.setOnClickListener {
            openImagePicker()
        }
        
        // Test connection button
        buttonTestConnection.setOnClickListener {
            testBackendConnection()
        }
        
        // Debug button
        findViewById<Button>(R.id.debugButton).setOnClickListener {

        }
    }
    
    private fun observeData() {
        // Observe sync status
        syncStatusManager.syncStatus.observe(this) { status ->
            unsyncedFormsCount.text = status.unsyncedFormsCount.toString()

            // Update last sync text
            if (status.lastSyncTimestamp != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                lastSyncText.text = "Last sync: ${dateFormat.format(Date(status.lastSyncTimestamp))}"
            } else {
                lastSyncText.text = "Last sync: Never"
            }

            // Update button states
            updateButtonStates(status.hasDataToSync)
        }
        
        // Observe upload progress
        uploadService.uploadProgress.observe(this) { progress ->
            if (progress.isUploading) {
                showProgress(true)
                uploadProgressBar.max = progress.totalItems
                uploadProgressBar.progress = progress.currentItem
                progressText.text = if (progress.totalItems > 0) {
                    "${progress.currentOperation}\n${progress.currentItem}/${progress.totalItems}"
                } else {
                    progress.currentOperation
                }
                
                // Disable buttons during upload
                buttonUploadForms.isEnabled = false
                buttonUploadImages.isEnabled = false
            } else {
                showProgress(false)
                
                // Re-check status after upload
                syncStatusManager.checkSyncStatus()
                
                // Show error if any
                if (progress.error != null) {
                    Toast.makeText(this, "Error: ${progress.error}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun checkNetworkStatus() {
        val isConnected = NetworkUtils.isNetworkAvailable(this)
        
        if (isConnected) {
            networkIcon.setImageResource(R.drawable.ic_wifi)
            networkStatusText.text = "Connected"
            networkStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            networkIcon.setImageResource(R.drawable.ic_wifi_off)
            networkStatusText.text = "No Internet Connection"
            networkStatusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
        
        // Update button states based on network
        updateButtonStatesForNetwork(isConnected)
    }
    
    private fun updateButtonStates(hasDataToSync: Boolean) {
        val isConnected = NetworkUtils.isNetworkAvailable(this)

        // Upload Forms button: enabled only if connected AND has unsynced forms
        val shouldEnableUploadForms = isConnected && hasDataToSync &&
                (unsyncedFormsCount.text.toString().toIntOrNull() ?: 0) > 0
        buttonUploadForms.isEnabled = shouldEnableUploadForms
        buttonUploadForms.alpha = if (shouldEnableUploadForms) 1.0f else 0.5f

        // Upload Images button: enabled only if connected (no data requirement)
        buttonUploadImages.isEnabled = isConnected
        buttonUploadImages.alpha = if (isConnected) 1.0f else 0.5f
    }
    
    private fun updateButtonStatesForNetwork(isConnected: Boolean) {
        if (!isConnected) {
            buttonUploadForms.isEnabled = false
            buttonUploadImages.isEnabled = false
            buttonUploadForms.alpha = 0.5f
            buttonUploadImages.alpha = 0.5f
        }
    }
    
    private fun uploadForms() {
        // First check if backend is reachable
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ReconUploadActivity, "Testing backend connection...", Toast.LENGTH_SHORT).show()
                
                // Test backend connectivity first
                val healthResponse = ApiClient.apiService.healthCheck()
                Log.d("ReconUpload", "Health check response: ${healthResponse.code()}")
                
                if (!healthResponse.isSuccessful) {
                    Toast.makeText(this@ReconUploadActivity, "Backend not reachable. Please check your API configuration.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val result = uploadService.uploadForms()
                if (result.isSuccess) {
                    Toast.makeText(this@ReconUploadActivity, result.getOrNull(), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ReconUploadActivity, "Upload failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ReconUpload", "Error in uploadForms", e)
                Toast.makeText(this@ReconUploadActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openImagePicker() {
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadImages(imageUris: List<Uri>) {
        lifecycleScope.launch {
            try {
                val result = uploadService.uploadImages(imageUris)
                if (result.isSuccess) {
                    Toast.makeText(this@ReconUploadActivity, result.getOrNull(), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ReconUploadActivity, "Upload failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReconUploadActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showProgress(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun testBackendConnection() {
        lifecycleScope.launch {
            try {
                buttonTestConnection.isEnabled = false
                buttonTestConnection.text = "Testing..."
                
                val result = ApiClient.testConnection()
                if (result.isSuccess) {
                    Toast.makeText(this@ReconUploadActivity, "✅ Backend connection successful!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ReconUploadActivity, "❌ Connection failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReconUploadActivity, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                buttonTestConnection.isEnabled = true
                buttonTestConnection.text = "Test Backend Connection"
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkNetworkStatus()
        syncStatusManager.checkSyncStatus()
    }
}
