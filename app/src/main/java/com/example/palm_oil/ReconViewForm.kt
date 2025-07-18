package com.example.palm_oil

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.ui.viewmodel.ReconFormViewModel

class ReconViewForm : AppCompatActivity() {
    private lateinit var viewModel: ReconFormViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReconFormAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_view_form)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ReconFormViewModel::class.java]
        
        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewForms)
        adapter = ReconFormAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Set up back button
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
        
        // Observe saved forms
        viewModel.allReconForms.observe(this) { forms ->
            adapter.submitList(forms)
        }
    }
}

class ReconFormAdapter : RecyclerView.Adapter<ReconFormAdapter.ReconFormViewHolder>() {
    private var forms = listOf<ReconFormEntity>()
    
    fun submitList(newForms: List<ReconFormEntity>) {
        forms = newForms
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ReconFormViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recon_form, parent, false)
        return ReconFormViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ReconFormViewHolder, position: Int) {
        holder.bind(forms[position])
    }
    
    override fun getItemCount(): Int = forms.size
    
    class ReconFormViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val textTreeId: TextView = itemView.findViewById(R.id.textTreeId)
        private val textPlotId: TextView = itemView.findViewById(R.id.textPlotId)
        private val textFruitsCount: TextView = itemView.findViewById(R.id.textFruitsCount)
        private val textHarvestDays: TextView = itemView.findViewById(R.id.textHarvestDays)
        private val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)
        private val imageView1: ImageView = itemView.findViewById(R.id.imageView1)
        private val imageView2: ImageView = itemView.findViewById(R.id.imageView2)
        private val imageView3: ImageView = itemView.findViewById(R.id.imageView3)
        
        fun bind(form: ReconFormEntity) {
            textTreeId.text = "Tree ID: ${form.treeId}"
            textPlotId.text = "Plot ID: ${form.plotId}"
            textFruitsCount.text = "Fruits: ${form.numberOfFruits}"
            textHarvestDays.text = "Harvest Days: ${form.harvestDays}"
            textTimestamp.text = "Recorded: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(form.createdAt)}"
            
            // Load images
            val imageViews = listOf(imageView1, imageView2, imageView3)
            val imagePaths = listOf(form.image1Path, form.image2Path, form.image3Path)
            
            imageViews.forEachIndexed { index, imageView ->
                val imagePath = imagePaths[index]
                if (imagePath != null && imagePath.isNotEmpty()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(imagePath)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = ImageView.VISIBLE
                    } catch (e: Exception) {
                        imageView.visibility = ImageView.GONE
                    }
                } else {
                    imageView.visibility = ImageView.GONE
                }
            }
        }
    }
}