package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        val buttonForm = findViewById<Button>(R.id.buttonForm)
        val buttonViewForm = findViewById<Button>(R.id.buttonViewForm)
        val buttonGallery = findViewById<Button>(R.id.buttonGallery)

        buttonForm.setOnClickListener {
            val intent = Intent(this, ReconForm::class.java)
            startActivity(intent)
        }
        buttonViewForm.setOnClickListener {
            val intent = Intent(this, ReconViewForm::class.java)
            startActivity(intent)
        }
        buttonGallery.setOnClickListener {
            val intent = Intent(this, ReconGallery::class.java)
            startActivity(intent)
        }
    }
}