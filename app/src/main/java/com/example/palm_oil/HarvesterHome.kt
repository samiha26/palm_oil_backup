package com.example.palm_oil

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
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
        val btnVirtualMap = findViewById<Button>(R.id.btnVirtualMap)
        val btnUpload = findViewById<Button>(R.id.btnUpload)
        val profileIcon = findViewById<ImageButton>(R.id.profileIcon)

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
        btnVirtualMap.setOnClickListener {
            val intent = Intent(this, HarvesterVirtualMapView::class.java)
            startActivity(intent)
        }
        btnUpload.setOnClickListener {
            val intent = Intent(this, HarvesterUploadActivity::class.java)
            startActivity(intent)
        }
        
        profileIcon.setOnClickListener {
            showProfileOptionsMenu(it)
        }
    }
    
    private fun showProfileOptionsMenu(view: android.view.View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun logout() {
        // Clear any user session data if needed
        val prefs = getSharedPreferences("palm_oil_prefs", MODE_PRIVATE)
        prefs.edit().remove("user_session").apply()
        
        // Create intent to redirect to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        
        // Clear back stack so user can't go back after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        // Show a toast message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Start MainActivity
        startActivity(intent)
        
        // Close current activity
        finish()
    }
}