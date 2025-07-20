package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HarvesterHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_harvester_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnProof = findViewById<Button>(R.id.btnProof)
        val btnViewProofs = findViewById<Button>(R.id.btnViewProofs)
        val btnDownloadMap = findViewById<Button>(R.id.btnDownloadMap)

        btnProof.setOnClickListener {
            val intent = Intent(this, HarvesterProof::class.java)
            startActivity(intent)
        }
        btnViewProofs.setOnClickListener {
            val intent = Intent(this, HarvesterViewProofs::class.java)
            startActivity(intent)
        }
        btnDownloadMap.setOnClickListener {
            val intent = Intent(this, HarvesterDownloadMap::class.java)
            startActivity(intent)
        }
    }
}