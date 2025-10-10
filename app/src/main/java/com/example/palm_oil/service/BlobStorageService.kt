package com.example.palm_oil.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.palm_oil.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service for uploading images to Vercel Blob Storage
 */
class BlobStorageService(private val context: Context) {

    companion object {
        private const val TAG = "BlobStorageService"
        private const val BLOB_API_URL = "https://blob.vercel-storage.com"
        private const val BLOB_API_VERSION = "7"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload an image file to Vercel Blob Storage
     * @param imageUri URI of the image to upload
     * @param filename Optional custom filename
     * @return URL of the uploaded image or null if failed
     */
    suspend fun uploadImage(imageUri: Uri, filename: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            // Get the blob token from BuildConfig
            val blobToken = BuildConfig.VERCEL_BLOB_TOKEN
            if (blobToken.isBlank()) {
                Log.e(TAG, "VERCEL_BLOB_TOKEN is not configured")
                return@withContext null
            }

            // Create a temporary file from URI
            val tempFile = createTempFileFromUri(imageUri, filename)
            if (tempFile == null) {
                Log.e(TAG, "Failed to create temp file from URI")
                return@withContext null
            }

            val actualFilename = filename ?: tempFile.name

            Log.d(TAG, "Uploading image: $actualFilename (${tempFile.length()} bytes)")

            // Read file bytes
            val fileBytes = tempFile.readBytes()
            val mimeType = when (tempFile.extension.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/jpeg"
            }

            // Build the request - PUT to Vercel Blob API with required headers
            // Based on Vercel Blob SDK source code structure
            val request = Request.Builder()
                .url("$BLOB_API_URL/$actualFilename")
                .addHeader("Authorization", "Bearer $blobToken")
                .addHeader("x-api-version", BLOB_API_VERSION)
                .addHeader("x-content-type", mimeType)
                .put(fileBytes.toRequestBody(mimeType.toMediaType()))
                .build()

            // Execute the request
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Upload failed: ${response.code} - $responseBody")
                    tempFile.delete()
                    return@withContext null
                }

                if (responseBody.isNullOrBlank()) {
                    Log.e(TAG, "Empty response from blob storage")
                    tempFile.delete()
                    return@withContext null
                }

                Log.d(TAG, "Blob storage response: $responseBody")

                // Parse the response to get the URL
                val jsonResponse = JSONObject(responseBody)
                val blobUrl = jsonResponse.optString("url")

                if (blobUrl.isBlank()) {
                    Log.e(TAG, "No URL in blob storage response")
                    tempFile.delete()
                    return@withContext null
                }

                Log.d(TAG, "Image uploaded successfully: $blobUrl")

                // Clean up temp file
                tempFile.delete()

                return@withContext blobUrl
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to blob storage", e)
            return@withContext null
        }
    }

    /**
     * Upload an image file directly from file path
     * @param filePath Path to the image file
     * @param filename Optional custom filename
     * @return URL of the uploaded image or null if failed
     */
    suspend fun uploadImageFromPath(filePath: String, filename: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return@withContext null
            }

            val blobToken = BuildConfig.VERCEL_BLOB_TOKEN
            if (blobToken.isBlank()) {
                Log.e(TAG, "VERCEL_BLOB_TOKEN is not configured")
                return@withContext null
            }

            val actualFilename = filename ?: file.name

            Log.d(TAG, "Uploading image from path: $actualFilename (${file.length()} bytes)")

            // Read file bytes
            val fileBytes = file.readBytes()
            val mimeType = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/jpeg"
            }

            // Build the request - PUT to Vercel Blob API with required headers
            // Based on Vercel Blob SDK source code structure
            val request = Request.Builder()
                .url("$BLOB_API_URL/$actualFilename")
                .addHeader("Authorization", "Bearer $blobToken")
                .addHeader("x-api-version", BLOB_API_VERSION)
                .addHeader("x-content-type", mimeType)
                .put(fileBytes.toRequestBody(mimeType.toMediaType()))
                .build()

            // Execute the request
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Upload failed: ${response.code} - $responseBody")
                    return@withContext null
                }

                if (responseBody.isNullOrBlank()) {
                    Log.e(TAG, "Empty response from blob storage")
                    return@withContext null
                }

                Log.d(TAG, "Blob storage response: $responseBody")

                // Parse the response to get the URL
                val jsonResponse = JSONObject(responseBody)
                val blobUrl = jsonResponse.optString("url")

                if (blobUrl.isBlank()) {
                    Log.e(TAG, "No URL in blob storage response")
                    return@withContext null
                }

                Log.d(TAG, "Image uploaded successfully: $blobUrl")

                return@withContext blobUrl
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image from path to blob storage", e)
            return@withContext null
        }
    }

    /**
     * Create a temporary file from a URI
     */
    private fun createTempFileFromUri(uri: Uri, filename: String?): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for URI: $uri")
                return null
            }

            val tempFile = File(context.cacheDir, filename ?: "temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from URI", e)
            null
        }
    }
}
