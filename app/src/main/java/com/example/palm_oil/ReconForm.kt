package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.ui.viewmodel.ReconFormViewModel

class ReconForm : AppCompatActivity() {
    private lateinit var viewModel: ReconFormViewModel
    private lateinit var editTreeId: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_form)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ReconFormViewModel::class.java]

        // Initialize views
        editTreeId = findViewById(R.id.editTreeId)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)

        btnNext.setOnClickListener {
            val treeId = editTreeId.text.toString().trim()
            if (treeId.isNotEmpty()) {
                // Save tree ID to ViewModel
                viewModel.setTreeId(treeId)
                
                // Navigate to ReconFormCapture
                val intent = Intent(this, ReconFormCapture::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a tree ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
}