package com.example.palm_oil.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.api.ImageItem
import com.example.palm_oil.api.ImageListRequest
import com.example.palm_oil.api.ReconFormRequest
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.utils.SyncStatusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class UploadProgress(
    val isUploading: Boolean = false,
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val currentOperation: String = "",
    val error: String? = null
)

class UploadService(private val context: Context) {
    
    private val database = PalmOilDatabase.getDatabase(context)
    private val syncStatusManager = SyncStatusManager(context)
    
    private val _uploadProgress = MutableLiveData<UploadProgress>()
    val uploadProgress: LiveData<UploadProgress> = _uploadProgress
    
    companion object {
        private const val TAG = "UploadService"
    }
    
    /**
     * Upload all unsynced forms to the backend
     */
    suspend fun uploadForms(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = true,
                    currentOperation = "Fetching unsynced forms..."
                )
            )
            
            val unsyncedForms = database.reconFormDao().getUnsyncedReconForms()
            
            if (unsyncedForms.isEmpty()) {
                return@withContext Result.success("No forms to upload")
            }
            
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = true,
                    totalItems = unsyncedForms.size,
                    currentOperation = "Uploading forms..."
                )
            )
            
            var successCount = 0
            var errorCount = 0
            
            unsyncedForms.forEachIndexed { index, form ->
                try {
                    _uploadProgress.postValue(
                        UploadProgress(
                            isUploading = true,
                            currentItem = index + 1,
                            totalItems = unsyncedForms.size,
                            currentOperation = "Uploading form for tree ${form.treeId}..."
                        )
                    )
                    
                    val request = ReconFormRequest(
                        treeId = form.treeId,
                        plotId = form.plotId,
                        numberOfFruits = form.numberOfFruits,
                        harvestDays = form.harvestDays,
                        clientId = android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        )
                    )
                    
                    val response = ApiClient.apiService.createReconForm(
                        apiKey = ApiClient.getApiKey(),
                        form = request
                    )
                    
                    Log.d(TAG, "API Response for tree ${form.treeId}: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        // Mark form as synced
                        database.reconFormDao().markFormAsSynced(form.id)
                        successCount++
                        Log.d(TAG, "Successfully uploaded form for tree ${form.treeId}")
                    } else {
                        errorCount++
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Failed to upload form for tree ${form.treeId}: ${response.code()} - $errorBody")
                    }
                    
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Error uploading form for tree ${form.treeId}", e)
                }
            }
            
            _uploadProgress.postValue(UploadProgress(isUploading = false))
            
            if (errorCount == 0) {
                syncStatusManager.updateLastSyncTimestamp()
                Result.success("Successfully uploaded $successCount forms")
            } else {
                Result.failure(Exception("Uploaded $successCount forms, failed $errorCount"))
            }
            
        } catch (e: Exception) {
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = false,
                    error = e.message
                )
            )
            Log.e(TAG, "Error in uploadForms", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload selected images from gallery to the backend
     */
    suspend fun uploadImages(imageUris: List<Uri>): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.success("No images to upload")
            }
            
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = true,
                    totalItems = imageUris.size,
                    currentOperation = "Preparing images for upload..."
                )
            )
            
            val imageItems = mutableListOf<ImageItem>()
            
            imageUris.forEachIndexed { index, uri ->
                try {
                    _uploadProgress.postValue(
                        UploadProgress(
                            isUploading = true,
                            currentItem = index + 1,
                            totalItems = imageUris.size,
                            currentOperation = "Processing image ${index + 1}..."
                        )
                    )
                    
                    // For now, we'll simulate image upload to blob storage
                    // In a real implementation, you would upload to blob storage first
                    val filename = getFileNameFromUri(uri)
                    val checksum = calculateFileChecksum(uri)
                    
                    // Simulated blob URL - replace with actual blob storage upload
                    val blobUrl = "https://blob.vercel-storage.com/simulated-${System.currentTimeMillis()}-${filename}"
                    
                    imageItems.add(
                        ImageItem(
                            url = blobUrl,
                            filename = filename,
                            timestamp = System.currentTimeMillis(),
                            checksum = checksum
                        )
                    )
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image at index $index", e)
                }
            }
            
            if (imageItems.isNotEmpty()) {
                _uploadProgress.postValue(
                    UploadProgress(
                        isUploading = true,
                        currentOperation = "Uploading image metadata to backend..."
                    )
                )
                
                val imageListRequest = ImageListRequest(images = imageItems)
                val response = ApiClient.apiService.uploadImageList(
                    apiKey = ApiClient.getApiKey(),
                    imageList = imageListRequest
                )
                
                if (response.isSuccessful) {
                    val result = response.body()
                    _uploadProgress.postValue(UploadProgress(isUploading = false))
                    
                    syncStatusManager.updateLastSyncTimestamp()
                    Result.success("Successfully processed ${result?.processed ?: 0} images")
                } else {
                    _uploadProgress.postValue(UploadProgress(isUploading = false))
                    Result.failure(Exception("Failed to upload images: ${response.code()}"))
                }
            } else {
                _uploadProgress.postValue(UploadProgress(isUploading = false))
                Result.failure(Exception("No valid images to upload"))
            }
            
        } catch (e: Exception) {
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = false,
                    error = e.message
                )
            )
            Log.e(TAG, "Error in uploadImages", e)
            Result.failure(e)
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                it.getString(nameIndex)
            } else {
                "image_${System.currentTimeMillis()}.jpg"
            }
        } ?: "image_${System.currentTimeMillis()}.jpg"
    }
    
    private fun calculateFileChecksum(uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            inputStream?.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum", e)
            "unknown_${System.currentTimeMillis()}"
        }
    }
}
