package com.example.palm_oil

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReconGallery : AppCompatActivity() {

    // Gallery picker for multiple images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "${uris.size} image(s) selected from gallery", Toast.LENGTH_SHORT).show()
            // Images selected, close activity
            finish()
        } else {
            // No images selected, just close
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Directly open phone's gallery without showing any UI
        openPhoneGallery()
    }

    private fun openPhoneGallery() {
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}