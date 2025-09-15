package com.example.palm_oil

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.repository.ReconFormRepository
import com.example.palm_oil.network.NetworkClient
import com.example.palm_oil.network.FormRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ReconUpload : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var repository: ReconFormRepository
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recon_upload)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize repository
        val database = PalmOilDatabase.getDatabase(this)
        repository = ReconFormRepository(database.reconFormDao())

        // Initialize views
        val buttonUploadForms = findViewById<Button>(R.id.buttonUploadForms)
        val buttonUploadImages = findViewById<Button>(R.id.buttonUploadImages)
        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        statusText = findViewById(R.id.statusText)

        // Initialize gallery launcher for image selection
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    handleImageSelection(data)
                }
            }
        }

        // Set up click listeners
        buttonUploadForms.setOnClickListener {
            uploadForms()
        }

        buttonUploadImages.setOnClickListener {
            openGalleryForImageSelection()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun uploadForms() {
        statusText.visibility = TextView.VISIBLE
        statusText.text = "Uploading forms..."

        lifecycleScope.launch {
            try {
                // Get all unsynced forms from database
                val unsyncedForms = repository.getUnsyncedReconForms()

                if (unsyncedForms.isEmpty()) {
                    statusText.text = "No forms to upload"
                    Toast.makeText(this@ReconUpload, "No forms to upload", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("ReconUpload", "Found ${unsyncedForms.size} forms to upload")

                // TODO: Implement actual HTTP upload to backend
                // For now, we'll just simulate the upload and mark forms as synced
                uploadFormsToBackend(unsyncedForms)

                statusText.text = "Forms uploaded successfully!"
                Toast.makeText(this@ReconUpload, "Forms uploaded successfully!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("ReconUpload", "Error uploading forms", e)
                statusText.text = "Error uploading forms"
                Toast.makeText(this@ReconUpload, "Error uploading forms: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun uploadFormsToBackend(forms: List<ReconFormEntity>) {
        // TODO: Replace this with actual HTTP calls to your backend /forms endpoint
        // Example payload structure based on backend.py:
        // {
        //   "treeId": "string",
        //   "plotId": "string",
        //   "numberOfFruits": integer,
        //   "harvestDays": integer
        // }

        for (form in forms) {
            try {
                Log.d("ReconUpload", "Uploading form: treeId=${form.treeId}, plotId=${form.plotId}")

                // Make HTTP POST request to /api/forms endpoint
                val request = FormRequest(
                    treeId = form.treeId,
                    plotId = form.plotId,
                    numberOfFruits = form.numberOfFruits,
                    harvestDays = form.harvestDays
                )

                val response = NetworkClient.apiService.createForm(
                    apiKey = BuildConfig.API_KEY,
                    request = request
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("ReconUpload", "Form uploaded successfully, server formId: ${responseBody?.formId}")
                    repository.markFormAsSynced(form.id)
                } else {
                    Log.e("ReconUpload", "Failed to upload form: ${response.code()} ${response.message()}")
                    throw Exception("Upload failed: ${response.code()} ${response.message()}")
                }
                Log.d("ReconUpload", "Successfully uploaded form ${form.id}")

            } catch (e: Exception) {
                Log.e("ReconUpload", "Failed to upload form ${form.id}", e)
                throw e
            }
        }
    }

    private fun openGalleryForImageSelection() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        try {
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("ReconUpload", "Error opening gallery", e)
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(data: Intent) {
        statusText.visibility = TextView.VISIBLE
        statusText.text = "Processing selected images..."

        lifecycleScope.launch {
            try {
                val imageUris = mutableListOf<Uri>()

                // Handle multiple image selection
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        imageUris.add(clipData.getItemAt(i).uri)
                    }
                } ?: data.data?.let { singleUri ->
                    // Handle single image selection
                    imageUris.add(singleUri)
                }

                if (imageUris.isEmpty()) {
                    statusText.text = "No images selected"
                    return@launch
                }

                Log.d("ReconUpload", "Selected ${imageUris.size} images for upload")

                // TODO: Upload images to blob storage
                uploadImagesToBlobStorage(imageUris)

                statusText.text = "Images uploaded successfully!"
                Toast.makeText(this@ReconUpload, "Images uploaded successfully!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("ReconUpload", "Error processing images", e)
                statusText.text = "Error processing images"
                Toast.makeText(this@ReconUpload, "Error processing images: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun uploadImagesToBlobStorage(imageUris: List<Uri>) {
        // This function:
        // 1. Converts Uri to byte array
        // 2. Uploads to Vercel Blob Storage
        // 3. Gets the public URL back
        // 4. Calls your backend's /api/image-list endpoint with the URLs

        val uploadedImages = mutableListOf<com.example.palm_oil.network.ImageRequest>()

        // Upload each image to blob storage
        for ((index, uri) in imageUris.withIndex()) {
            try {
                Log.d("ReconUpload", "Processing image ${index + 1}/${imageUris.size}")

                // Process and validate image
                val processedImageData = processImageFromUri(uri)
                if (processedImageData == null) {
                    Log.w("ReconUpload", "Could not process image at URI: $uri")
                    continue
                }

                // Upload to Vercel Blob Storage - create safe filename
                val timestamp = System.currentTimeMillis()
                val filename = "palm-oil-image-${timestamp}-${index}.jpg"
                val blobUrl = uploadToVercelBlob(processedImageData, filename)

                Log.d("ReconUpload", "Successfully uploaded ${processedImageData.size} bytes to blob storage: $blobUrl")

                // Add to list for backend upload
                uploadedImages.add(
                    com.example.palm_oil.network.ImageRequest(
                        url = blobUrl,
                        filename = filename,
                        timestamp = System.currentTimeMillis()
                    )
                )

                Log.d("ReconUpload", "Successfully processed image ${index + 1}")

            } catch (e: Exception) {
                Log.e("ReconUpload", "Failed to process image $index", e)
                throw e
            }
        }

        // Send all uploaded image URLs to your backend
        if (uploadedImages.isNotEmpty()) {
            Log.d("ReconUpload", "Sending ${uploadedImages.size} image URLs to backend")

            val imageListRequest = com.example.palm_oil.network.ImageListRequest(
                images = uploadedImages
            )

            val response = NetworkClient.apiService.uploadImageList(
                apiKey = BuildConfig.API_KEY,
                request = imageListRequest
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("ReconUpload", "Backend processed ${responseBody?.processed} images, ${responseBody?.errors?.size} errors")
            } else {
                Log.e("ReconUpload", "Failed to send images to backend: ${response.code()} ${response.message()}")
                throw Exception("Backend upload failed: ${response.code()} ${response.message()}")
            }
        }
    }

    private suspend fun uploadToVercelBlob(imageBytes: ByteArray, filename: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val blobToken = BuildConfig.VERCEL_BLOB_TOKEN

                if (blobToken.isEmpty()) {
                    throw Exception("Vercel Blob token not configured")
                }

                Log.d("ReconUpload", "Uploading $filename (${imageBytes.size} bytes) to Vercel Blob Storage")

                // Create HTTP client
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // Create request body with raw image bytes
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())

                // URL encode the filename and create proper pathname
                val encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                val url = "https://blob.vercel-storage.com/?pathname=$encodedFilename"

                Log.d("ReconUpload", "Uploading to URL: $url")

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $blobToken")
                    .addHeader("Content-Type", "image/jpeg")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    // Parse JSON response to get the URL
                    val jsonResponse = JSONObject(responseBody)
                    val blobUrl = jsonResponse.getString("url")

                    Log.d("ReconUpload", "Successfully uploaded to Vercel Blob: $blobUrl")
                    return@withContext blobUrl
                } else {
                    Log.e("ReconUpload", "Blob upload failed: ${response.code} - $responseBody")
                    throw Exception("Blob upload failed: ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ReconUpload", "Failed to upload to Vercel Blob", e)
                throw e
            }
        }
    }

    private fun processImageFromUri(uri: Uri): ByteArray? {
        return try {
            // Open input stream from URI
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.w("ReconUpload", "Could not open input stream for URI: $uri")
                return null
            }

            // Decode bitmap from input stream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.w("ReconUpload", "Could not decode bitmap from URI: $uri")
                return null
            }

            Log.d("ReconUpload", "Original bitmap: ${bitmap.width}x${bitmap.height}")

            // Optional: Resize image if it's too large (to reduce file size)
            val resizedBitmap = if (bitmap.width > 2048 || bitmap.height > 2048) {
                val scale = minOf(2048.0 / bitmap.width, 2048.0 / bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                Log.d("ReconUpload", "Resizing bitmap to: ${newWidth}x${newHeight}")
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            // Convert bitmap to JPEG byte array
            val outputStream = ByteArrayOutputStream()
            val success = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

            // Clean up bitmaps
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            bitmap.recycle()

            if (!success) {
                Log.e("ReconUpload", "Failed to compress bitmap to JPEG")
                return null
            }

            val imageBytes = outputStream.toByteArray()
            outputStream.close()

            Log.d("ReconUpload", "Processed image: ${imageBytes.size} bytes")
            return imageBytes

        } catch (e: Exception) {
            Log.e("ReconUpload", "Error processing image from URI: $uri", e)
            return null
        }
    }
}