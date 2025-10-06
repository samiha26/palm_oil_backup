package com.example.palm_oil

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.data.database.PalmOilDatabase
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var storageStatsText: TextView
    private lateinit var clearStorageButton: Button
    private lateinit var database: PalmOilDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        database = PalmOilDatabase.getDatabase(this)
        setupClickListeners()
        loadStorageStats()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        storageStatsText = findViewById(R.id.storageStatsText)
        clearStorageButton = findViewById(R.id.clearStorageButton)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        clearStorageButton.setOnClickListener {
            showClearStorageDialog()
        }
    }

    private fun loadStorageStats() {
        lifecycleScope.launch {
            try {
                val reconFormDao = database.reconFormDao()
                val harvesterProofDao = database.harvesterProofDao()
                val treeLocationDao = database.treeLocationDao()

                // Get counts
                val reconFormsCount = reconFormDao.getFormsCount()
                val unsyncedReconFormsCount = reconFormDao.getUnsyncedFormsCount()
                val harvesterProofsCount = harvesterProofDao.getProofsCount()
                val unsyncedHarvesterProofsCount = harvesterProofDao.getUnsyncedProofsCount()
                val treeLocationsCount = treeLocationDao.getTreeLocationsCount()
                val unsyncedTreeLocationsCount = treeLocationDao.getUnsyncedTreeLocationsCount()

                // Calculate image storage size
                val imageSize = calculateImageStorageSize()

                val statsText = buildString {
                    appendLine("📋 Recon Forms: $reconFormsCount")
                    if (unsyncedReconFormsCount > 0) {
                        appendLine("   └─ Unsynced: $unsyncedReconFormsCount")
                    }
                    appendLine()
                    appendLine("🌴 Tree Locations: $treeLocationsCount")
                    if (unsyncedTreeLocationsCount > 0) {
                        appendLine("   └─ Unsynced: $unsyncedTreeLocationsCount")
                    }
                    appendLine()
                    appendLine("📸 Harvester Proofs: $harvesterProofsCount")
                    if (unsyncedHarvesterProofsCount > 0) {
                        appendLine("   └─ Unsynced: $unsyncedHarvesterProofsCount")
                    }
                    appendLine()
                    appendLine("💾 Image Storage: ${formatFileSize(imageSize)}")
                }

                storageStatsText.text = statsText.trim()

            } catch (e: Exception) {
                storageStatsText.text = "Error loading storage stats: ${e.message}"
            }
        }
    }

    private fun calculateImageStorageSize(): Long {
        var totalSize = 0L

        // Check app's internal storage for images
        val imageDir = File(filesDir, "images")
        if (imageDir.exists() && imageDir.isDirectory) {
            imageDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
        }

        // Check external files directory
        externalCacheDir?.let { cacheDir ->
            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile && (file.extension == "jpg" || file.extension == "png")) {
                    totalSize += file.length()
                }
            }
        }

        return totalSize
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0))
        }
    }

    private fun showClearStorageDialog() {
        lifecycleScope.launch {
            val unsyncedReconForms = database.reconFormDao().getUnsyncedFormsCount()
            val unsyncedTrees = database.treeLocationDao().getUnsyncedTreeLocationsCount()
            val unsyncedProofs = database.harvesterProofDao().getUnsyncedProofsCount()

            val totalUnsynced = unsyncedReconForms + unsyncedTrees + unsyncedProofs

            val message = if (totalUnsynced > 0) {
                """
                ⚠️ WARNING: You have $totalUnsynced unsynced items:
                • $unsyncedReconForms recon forms
                • $unsyncedTrees tree locations
                • $unsyncedProofs harvester proofs

                These will be permanently deleted if you continue.

                Are you sure you want to clear all local storage?
                """.trimIndent()
            } else {
                """
                This will permanently delete all local data including:
                • All recon forms
                • All tree locations
                • All harvester proofs
                • All images

                Are you sure you want to continue?
                """.trimIndent()
            }

            AlertDialog.Builder(this@ProfileActivity)
                .setTitle("Clear Local Storage?")
                .setMessage(message)
                .setPositiveButton("Clear All Data") { _, _ ->
                    clearAllStorage()
                }
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }

    private fun clearAllStorage() {
        lifecycleScope.launch {
            try {
                // Show progress
                clearStorageButton.isEnabled = false
                clearStorageButton.text = "Clearing..."

                // Clear all database tables
                database.reconFormDao().deleteAllReconForms()
                database.treeLocationDao().deleteAllTreeLocations()
                database.harvesterProofDao().deleteAllHarvesterProofs()

                // Clear image files
                clearImageStorage()

                // Clear SharedPreferences if any
                clearSharedPreferences()

                // Reload stats
                loadStorageStats()

                // Show success message
                Toast.makeText(
                    this@ProfileActivity,
                    "All local storage cleared successfully!",
                    Toast.LENGTH_LONG
                ).show()

                clearStorageButton.isEnabled = true
                clearStorageButton.text = "CLEAR LOCAL STORAGE"

            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error clearing storage: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                clearStorageButton.isEnabled = true
                clearStorageButton.text = "CLEAR LOCAL STORAGE"
            }
        }
    }

    private fun clearImageStorage() {
        // Clear internal image directory
        val imageDir = File(filesDir, "images")
        if (imageDir.exists() && imageDir.isDirectory) {
            imageDir.deleteRecursively()
            imageDir.mkdirs() // Recreate the directory
        }

        // Clear cache
        cacheDir?.deleteRecursively()
        cacheDir?.mkdirs()

        // Clear external cache
        externalCacheDir?.deleteRecursively()
        externalCacheDir?.mkdirs()
    }

    private fun clearSharedPreferences() {
        // Clear default shared preferences
        val prefs = getSharedPreferences("palm_oil_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
