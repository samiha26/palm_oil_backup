package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect buttons by their IDs
        val buttonR = findViewById<Button>(R.id.buttonR)
        val buttonH = findViewById<Button>(R.id.buttonH)

        buttonR.setOnClickListener {
            // Redirect to ReconHomeActivity
            val intent = Intent(this, ReconHome::class.java)
            startActivity(intent)
        }
        buttonH.setOnClickListener {
            // Redirect to HarvesterHome
            val intent = Intent(this, HarvesterHome::class.java)
            startActivity(intent)
        }
    }
}