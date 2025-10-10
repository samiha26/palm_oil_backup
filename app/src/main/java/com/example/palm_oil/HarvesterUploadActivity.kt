package com.example.palm_oil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.service.UploadService
import com.example.palm_oil.utils.NetworkUtils
import com.example.palm_oil.utils.SyncStatusManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HarvesterUploadActivity : AppCompatActivity() {

    private lateinit var networkIcon: ImageView
    private lateinit var networkStatusText: TextView
    private lateinit var unsyncedProofsCount: TextView
    private lateinit var buttonUploadProofs: Button
    private lateinit var buttonTestConnection: Button
    private lateinit var progressLayout: LinearLayout
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var lastSyncText: TextView

    private lateinit var syncStatusManager: SyncStatusManager
    private lateinit var uploadService: UploadService
    private lateinit var database: PalmOilDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_upload) // Reusing recon upload layout
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
        loadHarvesterProofCounts()
    }

    private fun initializeViews() {
        networkIcon = findViewById(R.id.networkIcon)
        networkStatusText = findViewById(R.id.networkStatusText)
        unsyncedProofsCount = findViewById(R.id.unsyncedFormsCount) // Reusing this view
        buttonUploadProofs = findViewById(R.id.buttonUploadForms) // Reusing this button
        buttonTestConnection = findViewById(R.id.buttonTestConnection)
        progressLayout = findViewById(R.id.progressLayout)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        progressText = findViewById(R.id.progressText)
        lastSyncText = findViewById(R.id.lastSyncText)

        // Hide the upload images button since we don't need it for harvester proofs
        findViewById<Button>(R.id.buttonUploadImages).visibility = View.GONE
        findViewById<TextView>(R.id.localImagesCount).visibility = View.GONE

        // Update button text
        buttonUploadProofs.text = "Upload Harvester Proofs"
    }

    private fun initializeServices() {
        syncStatusManager = SyncStatusManager(this)
        uploadService = UploadService(this)
        database = PalmOilDatabase.getDatabase(this)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Upload proofs button
        buttonUploadProofs.setOnClickListener {
            uploadHarvesterProofs()
        }

        // Test connection button
        buttonTestConnection.setOnClickListener {
            testBackendConnection()
        }
    }

    private fun observeData() {
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
                buttonUploadProofs.isEnabled = false
            } else {
                showProgress(false)

                // Re-enable buttons
                buttonUploadProofs.isEnabled = true

                // Reload counts after upload
                loadHarvesterProofCounts()

                // Show error if any
                progress.error?.let { error ->
                    Toast.makeText(this, "Upload error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadHarvesterProofCounts() {
        lifecycleScope.launch {
            try {
                val unsyncedCount = database.harvesterProofDao().getUnsyncedProofsCount()
                unsyncedProofsCount.text = unsyncedCount.toString()

                // Update button states
                buttonUploadProofs.isEnabled = unsyncedCount > 0

                // Update last sync text
                val sharedPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
                val lastSyncTimestamp = sharedPrefs.getLong("last_sync_timestamp", 0L)
                if (lastSyncTimestamp != 0L) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    lastSyncText.text = "Last sync: ${dateFormat.format(Date(lastSyncTimestamp))}"
                } else {
                    lastSyncText.text = "Last sync: Never"
                }
            } catch (e: Exception) {
                Log.e("HarvesterUploadActivity", "Error loading counts", e)
                Toast.makeText(this@HarvesterUploadActivity, "Error loading harvester proof counts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadHarvesterProofs() {
        lifecycleScope.launch {
            try {
                val result = uploadService.uploadHarvesterProofs()

                result.onSuccess { message ->
                    Toast.makeText(this@HarvesterUploadActivity, message, Toast.LENGTH_LONG).show()
                    loadHarvesterProofCounts()
                }

                result.onFailure { error ->
                    Toast.makeText(this@HarvesterUploadActivity, "Upload failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("HarvesterUploadActivity", "Error uploading harvester proofs", e)
                Toast.makeText(this@HarvesterUploadActivity, "Upload error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkNetworkStatus() {
        val isConnected = NetworkUtils.isNetworkAvailable(this)

        if (isConnected) {
            networkIcon.setImageResource(android.R.drawable.presence_online)
            networkStatusText.text = "Online"
            networkStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            networkIcon.setImageResource(android.R.drawable.presence_offline)
            networkStatusText.text = "Offline"
            networkStatusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // Disable upload buttons if offline
        buttonUploadProofs.isEnabled = isConnected
        buttonTestConnection.isEnabled = isConnected
    }

    private fun testBackendConnection() {
        lifecycleScope.launch {
            try {
                showProgress(true)
                progressText.text = "Testing connection to backend..."

                val response = ApiClient.apiService.healthCheck()

                showProgress(false)

                if (response.isSuccessful) {
                    Toast.makeText(this@HarvesterUploadActivity, "✓ Backend connection successful!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@HarvesterUploadActivity, "✗ Backend returned error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showProgress(false)
                Log.e("HarvesterUploadActivity", "Connection test failed", e)
                Toast.makeText(this@HarvesterUploadActivity, "✗ Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showProgress(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        checkNetworkStatus()
        loadHarvesterProofCounts()
    }
}
