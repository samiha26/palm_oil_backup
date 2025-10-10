package com.example.palm_oil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.api.TreeLocationRequest
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.TreeLocationEntity
import kotlinx.coroutines.launch

class TreeUploadActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var unsyncedCountText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var treeListContainer: LinearLayout
    private lateinit var uploadButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var database: PalmOilDatabase
    private var unsyncedTrees: List<TreeLocationEntity> = emptyList()

    companion object {
        private const val TAG = "TreeUploadActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tree_upload)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        database = PalmOilDatabase.getDatabase(this)
        setupClickListeners()
        loadUnsyncedTrees()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        unsyncedCountText = findViewById(R.id.unsyncedCountText)
        totalCountText = findViewById(R.id.totalCountText)
        treeListContainer = findViewById(R.id.treeListContainer)
        uploadButton = findViewById(R.id.uploadButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        uploadButton.setOnClickListener {
            if (unsyncedTrees.isNotEmpty()) {
                uploadTrees()
            } else {
                Toast.makeText(this, "No trees to upload", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUnsyncedTrees() {
        lifecycleScope.launch {
            try {
                // Get unsynced trees
                unsyncedTrees = database.treeLocationDao().getUnsyncedTreeLocations()
                val totalCount = database.treeLocationDao().getTreeLocationsCount()

                Log.d(TAG, "Loaded ${unsyncedTrees.size} unsynced trees out of $totalCount total")

                // Update UI
                updateStats(unsyncedTrees.size, totalCount)
                displayTreeList(unsyncedTrees)

                // Enable/disable upload button
                uploadButton.isEnabled = unsyncedTrees.isNotEmpty()
                uploadButton.alpha = if (unsyncedTrees.isNotEmpty()) 1.0f else 0.5f

            } catch (e: Exception) {
                Log.e(TAG, "Error loading unsynced trees", e)
                Toast.makeText(this@TreeUploadActivity, "Error loading trees: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats(unsyncedCount: Int, totalCount: Int) {
        unsyncedCountText.text = "$unsyncedCount tree${if (unsyncedCount != 1) "s" else ""} waiting to upload"
        totalCountText.text = "Total: $totalCount tree${if (totalCount != 1) "s" else ""}"
    }

    private fun displayTreeList(trees: List<TreeLocationEntity>) {
        treeListContainer.removeAllViews()

        if (trees.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "All trees are synced!\nPlant new trees in the Virtual Map to upload."
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = android.view.Gravity.CENTER
                setPadding(32, 64, 32, 64)
            }
            treeListContainer.addView(emptyView)
            return
        }

        // Group trees by plot for better organization
        val treesByPlot = trees.groupBy { it.plotId }

        treesByPlot.forEach { (plotId, plotTrees) ->
            // Add plot header
            val plotHeader = TextView(this).apply {
                text = "Plot $plotId (${plotTrees.size} tree${if (plotTrees.size != 1) "s" else ""})"
                textSize = 18f
                setTextColor(resources.getColor(R.color.teal_700, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 24, 0, 16)
            }
            treeListContainer.addView(plotHeader)

            // Add trees in this plot
            plotTrees.forEach { tree ->
                val treeView = createTreeItemView(tree)
                treeListContainer.addView(treeView)
            }
        }
    }

    private fun createTreeItemView(tree: TreeLocationEntity): View {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 8f
            cardElevation = 2f
            setContentPadding(16, 16, 16, 16)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val treeIdText = TextView(this).apply {
            text = "Tree ID: ${tree.treeId}"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        contentLayout.addView(treeIdText)

        val coordinatesText = TextView(this).apply {
            text = if (tree.latitude != null && tree.longitude != null) {
                "GPS: ${String.format("%.6f", tree.latitude)}, ${String.format("%.6f", tree.longitude)}"
            } else {
                "Map: (${tree.xCoordinate.toInt()}, ${tree.yCoordinate.toInt()})"
            }
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(0, 4, 0, 0)
        }
        contentLayout.addView(coordinatesText)

        val dateText = TextView(this).apply {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(tree.createdAt)
            text = "Planted: $date"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(0, 4, 0, 0)
        }
        contentLayout.addView(dateText)

        if (!tree.notes.isNullOrBlank()) {
            val notesText = TextView(this).apply {
                text = "Notes: ${tree.notes}"
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(0, 4, 0, 0)
            }
            contentLayout.addView(notesText)
        }

        cardView.addView(contentLayout)
        return cardView
    }

    private fun uploadTrees() {
        if (unsyncedTrees.isEmpty()) {
            Toast.makeText(this, "No trees to upload", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress
        showLoading(true)

        lifecycleScope.launch {
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()

            try {
                val apiService = ApiClient.apiService
                val apiKey = ApiClient.getApiKey()

                // Upload each tree
                for (tree in unsyncedTrees) {
                    try {
                        Log.d(TAG, "Uploading tree: ${tree.treeId} in plot ${tree.plotId}")

                        val request = TreeLocationRequest(
                            treeId = tree.treeId,
                            plotId = tree.plotId,
                            xCoordinate = tree.xCoordinate.toDouble(),
                            yCoordinate = tree.yCoordinate.toDouble(),
                            latitude = tree.latitude,
                            longitude = tree.longitude,
                            notes = tree.notes,
                            clientId = android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            )
                        )

                        val response = apiService.createTreeLocation(apiKey, request)

                        if (response.isSuccessful) {
                            // Mark as synced
                            database.treeLocationDao().markTreeLocationAsSynced(
                                tree.id,
                                System.currentTimeMillis()
                            )
                            successCount++
                            Log.d(TAG, "Successfully uploaded tree: ${tree.treeId}")
                        } else {
                            failureCount++
                            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                            Log.e(TAG, "Failed to upload tree ${tree.treeId}: ${response.code()} - $errorMsg")
                            errors.add("${tree.treeId}: ${response.code()}")
                        }

                    } catch (e: Exception) {
                        failureCount++
                        Log.e(TAG, "Exception uploading tree ${tree.treeId}", e)
                        errors.add("${tree.treeId}: ${e.message}")
                    }
                }

                // Show results
                showLoading(false)

                val message = buildString {
                    append("Upload complete!\n")
                    append("✓ Success: $successCount\n")
                    if (failureCount > 0) {
                        append("✗ Failed: $failureCount\n")
                        if (errors.isNotEmpty()) {
                            append("\nErrors:\n")
                            errors.take(5).forEach { append("- $it\n") }
                            if (errors.size > 5) {
                                append("... and ${errors.size - 5} more")
                            }
                        }
                    }
                }

                Toast.makeText(this@TreeUploadActivity, message, Toast.LENGTH_LONG).show()

                // Reload the list
                loadUnsyncedTrees()

            } catch (e: Exception) {
                showLoading(false)
                Log.e(TAG, "Error during upload process", e)
                Toast.makeText(
                    this@TreeUploadActivity,
                    "Upload error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        uploadButton.isEnabled = !show
        uploadButton.alpha = if (show) 0.5f else 1.0f
    }
}
