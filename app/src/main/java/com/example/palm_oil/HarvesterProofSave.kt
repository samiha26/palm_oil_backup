package com.example.palm_oil

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HarvesterProofSave : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_proof_save)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val imagePath = intent.getStringExtra("imagePath")

        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
        } else {
            // Optionally set a placeholder or hide the ImageView
            imageView.setImageResource(android.R.color.darker_gray)
        }

        backButton.setOnClickListener {
            finish()
        }
        saveButton.setOnClickListener {
            val intent = Intent(this, HarvesterProof::class.java)
            startActivity(intent)
        }
    }
}