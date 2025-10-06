package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReconHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileIcon = findViewById<ImageButton>(R.id.profileIcon)
        val buttonForm = findViewById<Button>(R.id.buttonForm)
        val buttonViewForm = findViewById<Button>(R.id.buttonViewForm)
        val buttonVirtualMap = findViewById<Button>(R.id.buttonVirtualMap)
        val buttonUpload = findViewById<Button>(R.id.buttonUpload)
        val buttonUploadTrees = findViewById<Button>(R.id.buttonUploadTrees)

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        buttonForm.setOnClickListener {
            val intent = Intent(this, ReconForm::class.java)
            startActivity(intent)
        }
        buttonViewForm.setOnClickListener {
            val intent = Intent(this, ReconViewForm::class.java)
            startActivity(intent)
        }
        buttonVirtualMap.setOnClickListener {
            val intent = Intent(this, VirtualMapActivity::class.java)
            startActivity(intent)
        }
        buttonUpload.setOnClickListener {
            val intent = Intent(this, ReconUploadActivity::class.java)
            startActivity(intent)
        }
        buttonUploadTrees.setOnClickListener {
            val intent = Intent(this, TreeUploadActivity::class.java)
            startActivity(intent)
        }
    }
}