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
    private val blobStorageService = BlobStorageService(context)

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
            val errors = mutableListOf<String>()
            
            imageUris.forEachIndexed { index, uri ->
                try {
                    _uploadProgress.postValue(
                        UploadProgress(
                            isUploading = true,
                            currentItem = index + 1,
                            totalItems = imageUris.size,
                            currentOperation = "Uploading image ${index + 1} to blob storage..."
                        )
                    )

                    val filename = getFileNameFromUri(uri)
                    val checksum = calculateFileChecksum(uri)

                    // Upload image to Vercel Blob Storage
                    val blobUrl = blobStorageService.uploadImage(
                        imageUri = uri,
                        filename = "recon_${System.currentTimeMillis()}_${filename}"
                    )

                    if (blobUrl != null) {
                        Log.d(TAG, "Image uploaded to blob storage: $blobUrl")
                        imageItems.add(
                            ImageItem(
                                url = blobUrl,
                                filename = filename,
                                timestamp = System.currentTimeMillis(),
                                checksum = checksum
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to upload image to blob storage at index $index")
                        // Still add with a placeholder to track the failure
                        errors.add("Image ${index + 1}: Failed to upload to blob storage")
                    }

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

                    val successMessage = "Successfully processed ${result?.processed ?: 0} images"
                    val errorMessage = if (errors.isNotEmpty()) {
                        "\nErrors: ${errors.joinToString(", ")}"
                    } else {
                        ""
                    }

                    Result.success(successMessage + errorMessage)
                } else {
                    _uploadProgress.postValue(UploadProgress(isUploading = false))
                    Result.failure(Exception("Failed to upload images: ${response.code()}"))
                }
            } else {
                _uploadProgress.postValue(UploadProgress(isUploading = false))
                val errorMessage = if (errors.isNotEmpty()) {
                    "No valid images to upload. Errors: ${errors.joinToString(", ")}"
                } else {
                    "No valid images to upload"
                }
                Result.failure(Exception(errorMessage))
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

    /**
     * Upload all unsynced harvester proofs to the backend
     */
    suspend fun uploadHarvesterProofs(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = true,
                    currentOperation = "Fetching unsynced harvester proofs..."
                )
            )

            val unsyncedProofs = database.harvesterProofDao().getUnsyncedHarvesterProofs()

            if (unsyncedProofs.isEmpty()) {
                return@withContext Result.success("No harvester proofs to upload")
            }

            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = true,
                    totalItems = unsyncedProofs.size,
                    currentOperation = "Uploading harvester proofs..."
                )
            )

            var successCount = 0
            var errorCount = 0

            unsyncedProofs.forEachIndexed { index, proof ->
                try {
                    _uploadProgress.postValue(
                        UploadProgress(
                            isUploading = true,
                            currentItem = index + 1,
                            totalItems = unsyncedProofs.size,
                            currentOperation = "Uploading proof for tree ${proof.treeId}..."
                        )
                    )

                    // Upload image to blob storage first
                    var imageUrl: String? = null
                    if (!proof.imagePath.isNullOrBlank()) {
                        _uploadProgress.postValue(
                            UploadProgress(
                                isUploading = true,
                                currentItem = index + 1,
                                totalItems = unsyncedProofs.size,
                                currentOperation = "Uploading image for tree ${proof.treeId} to blob storage..."
                            )
                        )

                        imageUrl = blobStorageService.uploadImageFromPath(
                            filePath = proof.imagePath,
                            filename = "harvester_${proof.treeId}_${System.currentTimeMillis()}.jpg"
                        )

                        if (imageUrl == null) {
                            Log.w(TAG, "Failed to upload image to blob storage for tree ${proof.treeId}, proceeding without image URL")
                        } else {
                            Log.d(TAG, "Image uploaded to blob storage: $imageUrl")
                        }
                    }

                    val request = com.example.palm_oil.api.HarvesterProofRequest(
                        treeId = proof.treeId,
                        plotId = proof.plotId,
                        imageUrl = imageUrl,
                        latitude = proof.locationLatitude,
                        longitude = proof.locationLongitude,
                        accuracy = null, // No accuracy field in HarvesterProofEntity
                        notes = proof.notes,
                        harvesterId = null, // Can be added later if needed
                        clientId = android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        )
                    )

                    val response = ApiClient.apiService.createHarvesterProof(
                        apiKey = ApiClient.getApiKey(),
                        request = request
                    )

                    Log.d(TAG, "API Response for harvester proof tree ${proof.treeId}: ${response.code()}")

                    if (response.isSuccessful) {
                        // Mark proof as synced
                        database.harvesterProofDao().markProofAsSynced(proof.id)
                        successCount++
                        Log.d(TAG, "Successfully uploaded harvester proof for tree ${proof.treeId}")
                    } else {
                        errorCount++
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Failed to upload harvester proof for tree ${proof.treeId}: ${response.code()} - $errorBody")
                    }

                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Error uploading harvester proof for tree ${proof.treeId}", e)
                }
            }

            _uploadProgress.postValue(UploadProgress(isUploading = false))

            if (errorCount == 0) {
                syncStatusManager.updateLastSyncTimestamp()
                Result.success("Successfully uploaded $successCount harvester proofs")
            } else {
                Result.failure(Exception("Uploaded $successCount harvester proofs, failed $errorCount"))
            }

        } catch (e: Exception) {
            _uploadProgress.postValue(
                UploadProgress(
                    isUploading = false,
                    error = e.message
                )
            )
            Log.e(TAG, "Error in uploadHarvesterProofs", e)
            Result.failure(e)
        }
    }
}
