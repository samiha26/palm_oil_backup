package com.example.palm_oil.network

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.palm_oil.api.ApiClient
import com.example.palm_oil.data.database.HarvesterProofEntity
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.database.TreeLocationEntity
import com.example.palm_oil.data.repository.HarvesterProofRepository
import com.example.palm_oil.data.repository.ReconFormRepository
import com.example.palm_oil.data.repository.TreeLocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

data class SyncResult(
    val type: SyncType,
    val totalItems: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<SyncError> = emptyList(),
    val isSuccess: Boolean = errorCount == 0
)

data class SyncError(
    val itemId: Long,
    val itemType: SyncType,
    val errorMessage: String,
    val retryCount: Int = 0
)

enum class SyncType {
    RECON_FORMS,
    HARVESTER_PROOFS,
    TREE_LOCATIONS,
    IMAGES
}

data class SyncProgress(
    val type: SyncType,
    val current: Int,
    val total: Int,
    val message: String
)

class SyncService(
    private val context: Context,
    private val reconFormRepository: ReconFormRepository,
    private val harvesterProofRepository: HarvesterProofRepository,
    private val treeLocationRepository: TreeLocationRepository
) {

    companion object {
        private const val TAG = "SyncService"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val REQUEST_TIMEOUT_SECONDS = 60L
    }

    private val apiService = ApiClient.apiService
    private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    // Callback interfaces for progress tracking
    var onProgressUpdate: ((SyncProgress) -> Unit)? = null
    var onSyncComplete: ((List<SyncResult>) -> Unit)? = null
    var onError: ((SyncError) -> Unit)? = null

    /**
     * Sync all data types in sequence
     */
    suspend fun syncAllData(): List<SyncResult> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting comprehensive sync of all data")
        val results = mutableListOf<SyncResult>()

        try {
            // 1. Sync Recon Forms
            val formsResult = syncReconForms()
            results.add(formsResult)

            // 2. Sync Harvester Proofs
            val proofsResult = syncHarvesterProofs()
            results.add(proofsResult)

            // 3. Sync Tree Locations
            val locationsResult = syncTreeLocations()
            results.add(locationsResult)

            Log.i(TAG, "Sync completed. Results: ${results.size} operations")
            onSyncComplete?.invoke(results)

        } catch (e: Exception) {
            Log.e(TAG, "Critical error during sync", e)
            onError?.invoke(SyncError(0, SyncType.RECON_FORMS, "Critical sync error: ${e.message}"))
        }

        return@withContext results
    }

    /**
     * Sync reconnaissance forms with enhanced error handling
     */
    suspend fun syncReconForms(): SyncResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting sync of reconnaissance forms")

        val unsyncedForms = reconFormRepository.getUnsyncedReconForms()
        val totalCount = unsyncedForms.size
        var successCount = 0
        val errors = mutableListOf<SyncError>()

        if (totalCount == 0) {
            Log.i(TAG, "No unsynced recon forms found")
            return@withContext SyncResult(SyncType.RECON_FORMS, 0, 0, 0)
        }

        onProgressUpdate?.invoke(SyncProgress(SyncType.RECON_FORMS, 0, totalCount, "Syncing recon forms..."))

        for ((index, form) in unsyncedForms.withIndex()) {
            try {
                val success = syncSingleReconForm(form)
                if (success) {
                    successCount++
                } else {
                    errors.add(SyncError(form.id, SyncType.RECON_FORMS, "Failed to sync form"))
                }

                onProgressUpdate?.invoke(
                    SyncProgress(SyncType.RECON_FORMS, index + 1, totalCount,
                    "Synced ${index + 1}/$totalCount recon forms")
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing recon form ${form.id}", e)
                errors.add(SyncError(form.id, SyncType.RECON_FORMS, e.message ?: "Unknown error"))
                onError?.invoke(errors.last())
            }
        }

        Log.i(TAG, "Recon forms sync complete: $successCount/$totalCount successful")
        return@withContext SyncResult(SyncType.RECON_FORMS, totalCount, successCount, errors.size, errors)
    }

    /**
     * Sync harvester proofs with image upload handling
     */
    suspend fun syncHarvesterProofs(): SyncResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting sync of harvester proofs")

        val unsyncedProofs = harvesterProofRepository.getUnsyncedHarvesterProofs()
        val totalCount = unsyncedProofs.size
        var successCount = 0
        val errors = mutableListOf<SyncError>()

        if (totalCount == 0) {
            Log.i(TAG, "No unsynced harvester proofs found")
            return@withContext SyncResult(SyncType.HARVESTER_PROOFS, 0, 0, 0)
        }

        onProgressUpdate?.invoke(SyncProgress(SyncType.HARVESTER_PROOFS, 0, totalCount, "Syncing harvester proofs..."))

        for ((index, proof) in unsyncedProofs.withIndex()) {
            try {
                val success = syncSingleHarvesterProof(proof)
                if (success) {
                    successCount++
                } else {
                    errors.add(SyncError(proof.id, SyncType.HARVESTER_PROOFS, "Failed to sync proof"))
                }

                onProgressUpdate?.invoke(
                    SyncProgress(SyncType.HARVESTER_PROOFS, index + 1, totalCount,
                    "Synced ${index + 1}/$totalCount harvester proofs")
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing harvester proof ${proof.id}", e)
                errors.add(SyncError(proof.id, SyncType.HARVESTER_PROOFS, e.message ?: "Unknown error"))
                onError?.invoke(errors.last())
            }
        }

        Log.i(TAG, "Harvester proofs sync complete: $successCount/$totalCount successful")
        return@withContext SyncResult(SyncType.HARVESTER_PROOFS, totalCount, successCount, errors.size, errors)
    }

    /**
     * Sync tree locations
     */
    suspend fun syncTreeLocations(): SyncResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting sync of tree locations")

        val unsyncedLocations = treeLocationRepository.getUnsyncedTreeLocations()
        val totalCount = unsyncedLocations.size
        var successCount = 0
        val errors = mutableListOf<SyncError>()

        if (totalCount == 0) {
            Log.i(TAG, "No unsynced tree locations found")
            return@withContext SyncResult(SyncType.TREE_LOCATIONS, 0, 0, 0)
        }

        onProgressUpdate?.invoke(SyncProgress(SyncType.TREE_LOCATIONS, 0, totalCount, "Syncing tree locations..."))

        for ((index, location) in unsyncedLocations.withIndex()) {
            try {
                val success = syncSingleTreeLocation(location)
                if (success) {
                    successCount++
                } else {
                    errors.add(SyncError(location.id, SyncType.TREE_LOCATIONS, "Failed to sync location"))
                }

                onProgressUpdate?.invoke(
                    SyncProgress(SyncType.TREE_LOCATIONS, index + 1, totalCount,
                    "Synced ${index + 1}/$totalCount tree locations")
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing tree location ${location.id}", e)
                errors.add(SyncError(location.id, SyncType.TREE_LOCATIONS, e.message ?: "Unknown error"))
                onError?.invoke(errors.last())
            }
        }

        Log.i(TAG, "Tree locations sync complete: $successCount/$totalCount successful")
        return@withContext SyncResult(SyncType.TREE_LOCATIONS, totalCount, successCount, errors.size, errors)
    }

    /**
     * Sync a single recon form with retry logic
     */
    private suspend fun syncSingleReconForm(form: ReconFormEntity, retryCount: Int = 0): Boolean {
        return try {
            Log.d(TAG, "Syncing recon form: ${form.treeId} (attempt ${retryCount + 1})")

            val request = com.example.palm_oil.api.ReconFormRequest(
                treeId = form.treeId,
                plotId = form.plotId,
                numberOfFruits = form.numberOfFruits,
                harvestDays = form.harvestDays,
                clientId = deviceId
            )

            val response = apiService.createReconForm(
                apiKey = ApiClient.getApiKey(),
                form = request
            )

            if (response.isSuccessful) {
                reconFormRepository.markFormAsSynced(form.id)
                Log.d(TAG, "Successfully synced recon form ${form.id}")
                true
            } else {
                Log.w(TAG, "Server rejected recon form ${form.id}: ${response.code()}")
                handleServerError(response, form.id, SyncType.RECON_FORMS, retryCount)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing recon form ${form.id}", e)
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY_MS * (retryCount + 1))
                syncSingleReconForm(form, retryCount + 1)
            } else {
                false
            }
        }
    }

    /**
     * Sync a single harvester proof with image upload
     */
    private suspend fun syncSingleHarvesterProof(proof: HarvesterProofEntity, retryCount: Int = 0): Boolean {
        return try {
            Log.d(TAG, "Syncing harvester proof: ${proof.treeId} (attempt ${retryCount + 1})")

            // Handle image upload if needed
            var imageUrl: String? = null
            if (proof.imagePath.isNotEmpty() && !proof.imagePath.startsWith("https://")) {
                imageUrl = uploadImageToBlob(proof.imagePath, "harvester-proof")
                if (imageUrl == null) {
                    Log.w(TAG, "Failed to upload image for harvester proof ${proof.id}")
                    return false
                }
            } else if (proof.imagePath.startsWith("https://")) {
                imageUrl = proof.imagePath
            }

            val request = com.example.palm_oil.api.HarvesterProofRequest(
                treeId = proof.treeId,
                plotId = proof.plotId,
                imageUrl = imageUrl,
                latitude = proof.locationLatitude,
                longitude = proof.locationLongitude,
                accuracy = null,
                notes = proof.notes,
                harvesterId = null,
                clientId = deviceId
            )

            val response = apiService.createHarvesterProof(
                apiKey = ApiClient.getApiKey(),
                request = request
            )

            if (response.isSuccessful) {
                harvesterProofRepository.markProofAsSynced(proof.id)
                Log.d(TAG, "Successfully synced harvester proof ${proof.id}")
                true
            } else {
                Log.w(TAG, "Server rejected harvester proof ${proof.id}: ${response.code()}")
                handleServerError(response, proof.id, SyncType.HARVESTER_PROOFS, retryCount)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing harvester proof ${proof.id}", e)
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY_MS * (retryCount + 1))
                syncSingleHarvesterProof(proof, retryCount + 1)
            } else {
                false
            }
        }
    }

    /**
     * Sync a single tree location
     */
    private suspend fun syncSingleTreeLocation(location: TreeLocationEntity, retryCount: Int = 0): Boolean {
        return try {
            Log.d(TAG, "Syncing tree location: ${location.treeId} (attempt ${retryCount + 1})")

            val request = com.example.palm_oil.api.TreeLocationRequest(
                treeId = location.treeId,
                plotId = location.plotId,
                xCoordinate = location.xCoordinate.toDouble(),
                yCoordinate = location.yCoordinate.toDouble(),
                latitude = location.latitude,
                longitude = location.longitude,
                notes = location.notes,
                clientId = deviceId
            )

            val response = apiService.createTreeLocation(
                apiKey = ApiClient.getApiKey(),
                request = request
            )

            if (response.isSuccessful) {
                treeLocationRepository.markTreeLocationAsSynced(location.id)
                Log.d(TAG, "Successfully synced tree location ${location.id}")
                true
            } else {
                Log.w(TAG, "Server rejected tree location ${location.id}: ${response.code()}")
                handleServerError(response, location.id, SyncType.TREE_LOCATIONS, retryCount)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing tree location ${location.id}", e)
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY_MS * (retryCount + 1))
                syncSingleTreeLocation(location, retryCount + 1)
            } else {
                false
            }
        }
    }

    /**
     * Upload image file to Vercel Blob Storage
     */
    private suspend fun uploadImageToBlob(imagePath: String, prefix: String): String? {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                Log.w(TAG, "Image file does not exist: $imagePath")
                return null
            }

            val imageBytes = imageFile.readBytes()
            val mimeType = when (imageFile.extension.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/jpeg"
            }

            val filename = "$prefix-${System.currentTimeMillis()}.${imageFile.extension}"

            uploadToVercelBlob(imageBytes, filename, mimeType)

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to blob storage", e)
            null
        }
    }

    /**
     * Upload to Vercel Blob Storage with enhanced error handling
     */
    private suspend fun uploadToVercelBlob(imageBytes: ByteArray, filename: String, mimeType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val blobToken = ApiClient.getVercelBlobToken()
                if (blobToken.isEmpty()) {
                    Log.e(TAG, "Vercel Blob token not configured")
                    return@withContext null
                }

                Log.d(TAG, "Uploading $filename (${imageBytes.size} bytes) to Vercel Blob Storage")

                val client = OkHttpClient.Builder()
                    .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
                val safePath = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val url = "https://blob.vercel-storage.com/$safePath"

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $blobToken")
                    .addHeader("Content-Type", mimeType)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val blobUrl = jsonResponse.getString("url")
                    Log.d(TAG, "Successfully uploaded to Vercel Blob: $blobUrl")
                    return@withContext blobUrl
                } else {
                    Log.e(TAG, "Blob upload failed: ${response.code} - $responseBody")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload to Vercel Blob", e)
                return@withContext null
            }
        }
    }

    /**
     * Handle server errors with appropriate retry logic
     */
    private suspend fun handleServerError(response: Response<*>, itemId: Long, type: SyncType, retryCount: Int): Boolean {
        return when (response.code()) {
            409 -> {
                // Conflict - item already exists, mark as synced
                Log.i(TAG, "Item $itemId already exists on server, marking as synced")
                markItemAsSynced(itemId, type)
                true
            }
            422 -> {
                // Validation error - don't retry
                Log.w(TAG, "Validation error for item $itemId, not retrying")
                false
            }
            429 -> {
                // Rate limited - retry with longer delay
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    Log.i(TAG, "Rate limited, retrying item $itemId after delay")
                    delay(RETRY_DELAY_MS * 2)
                    true // Signal to retry
                } else {
                    false
                }
            }
            in 500..599 -> {
                // Server error - retry
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    Log.i(TAG, "Server error, retrying item $itemId")
                    delay(RETRY_DELAY_MS)
                    true // Signal to retry
                } else {
                    false
                }
            }
            else -> {
                Log.w(TAG, "Unhandled server error ${response.code()} for item $itemId")
                false
            }
        }
    }

    /**
     * Mark an item as synced in the appropriate repository
     */
    private suspend fun markItemAsSynced(itemId: Long, type: SyncType) {
        when (type) {
            SyncType.RECON_FORMS -> reconFormRepository.markFormAsSynced(itemId)
            SyncType.HARVESTER_PROOFS -> harvesterProofRepository.markProofAsSynced(itemId)
            SyncType.TREE_LOCATIONS -> treeLocationRepository.markTreeLocationAsSynced(itemId)
            SyncType.IMAGES -> { /* Handle if needed */ }
        }
    }

    /**
     * Check network connectivity and server health
     */
    suspend fun checkConnectivity(): Boolean {
        return try {
            val response = apiService.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Connectivity check failed", e)
            false
        }
    }

    /**
     * Get sync statistics
     */
    suspend fun getSyncStatistics(): Map<SyncType, Pair<Int, Int>> {
        return mapOf(
            SyncType.RECON_FORMS to Pair(
                reconFormRepository.getUnsyncedReconForms().size,
                reconFormRepository.getAllReconFormsSync().size
            ),
            SyncType.HARVESTER_PROOFS to Pair(
                harvesterProofRepository.getUnsyncedHarvesterProofs().size,
                harvesterProofRepository.getAllHarvesterProofsSync().size
            ),
            SyncType.TREE_LOCATIONS to Pair(
                treeLocationRepository.getUnsyncedTreeLocations().size,
                treeLocationRepository.getAllTreeLocationsSync().size
            )
        )
    }
}