package com.example.palm_oil

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.palm_oil.data.database.HarvesterProofEntity
import com.example.palm_oil.ui.harvesterproof.HarvesterProofViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HarvesterViewProofs : AppCompatActivity() {
    
    private lateinit var viewModel: HarvesterProofViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HarvesterProofListAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_harvester_view_proofs)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HarvesterProofViewModel::class.java]
        
        initializeViews()
        setupObservers()
        
        // Load data
        loadHarvesterProofs()
    }

    private fun initializeViews() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        recyclerView = findViewById(R.id.recyclerViewForms)
        
        // Setup RecyclerView
        adapter = HarvesterProofListAdapter { harvesterProof ->
            // Handle item click if needed
            Log.d("HarvesterViewProofs", "Clicked on proof: ${harvesterProof.treeId} - ${harvesterProof.plotId}")
            Toast.makeText(this, "Tree ID: ${harvesterProof.treeId}, Plot ID: ${harvesterProof.plotId}", Toast.LENGTH_SHORT).show()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        // Observe all harvester proofs
        viewModel.allHarvesterProofs.observe(this) { proofs ->
            Log.d("HarvesterViewProofs", "Received ${proofs.size} harvester proofs")
            adapter.updateProofs(proofs)
            
            // Show/hide message based on data
            if (proofs.isEmpty()) {
                Toast.makeText(this, "No harvester proofs found", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe errors
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun loadHarvesterProofs() {
        // The LiveData observer will automatically update the list
        viewModel.refreshData()
    }
}

class HarvesterProofListAdapter(
    private val onItemClick: (HarvesterProofEntity) -> Unit
) : RecyclerView.Adapter<HarvesterProofListAdapter.HarvesterProofViewHolder>() {
    
    private var proofs = listOf<HarvesterProofEntity>()
    
    fun updateProofs(newProofs: List<HarvesterProofEntity>) {
        proofs = newProofs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HarvesterProofViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_harvester_proof, parent, false)
        return HarvesterProofViewHolder(view)
    }

    override fun onBindViewHolder(holder: HarvesterProofViewHolder, position: Int) {
        holder.bind(proofs[position])
    }

    override fun getItemCount() = proofs.size

    inner class HarvesterProofViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val treeIdText: TextView = itemView.findViewById(R.id.treeIdText)
        private val plotIdText: TextView = itemView.findViewById(R.id.plotIdText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val syncStatusText: TextView = itemView.findViewById(R.id.syncStatusText)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(proof: HarvesterProofEntity) {
            treeIdText.text = "Tree ID: ${proof.treeId}"
            plotIdText.text = "Plot ID: ${proof.plotId}"
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            dateText.text = dateFormat.format(Date(proof.createdAt))
            
            // Sync status
            syncStatusText.text = if (proof.isSynced) "Synced" else "Not Synced"
            syncStatusText.setTextColor(
                if (proof.isSynced) 
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else 
                    itemView.context.getColor(android.R.color.holo_red_dark)
            )
            
            // Load image
            if (proof.imagePath.isNotEmpty() && File(proof.imagePath).exists()) {
                try {
                    imageView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(proof.imagePath))
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (e: Exception) {
                    Log.e("HarvesterProofListAdapter", "Error loading image", e)
                    imageView.setImageResource(android.R.color.darker_gray)
                }
            } else {
                imageView.setImageResource(android.R.color.darker_gray)
            }
            
            itemView.setOnClickListener {
                onItemClick(proof)
            }
        }
    }
}