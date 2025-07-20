package com.example.palm_oil.ui.harvesterproof

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.palm_oil.data.database.HarvesterProofEntity
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.repository.HarvesterProofRepository
import kotlinx.coroutines.launch

class HarvesterProofViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: HarvesterProofRepository
    
    // Form state
    private val _treeId = MutableLiveData<String>()
    val treeId: LiveData<String> = _treeId
    
    private val _plotId = MutableLiveData<String>()
    val plotId: LiveData<String> = _plotId
    
    private val _imagePath = MutableLiveData<String?>()
    val imagePath: LiveData<String?> = _imagePath
    
    private val _notes = MutableLiveData<String>()
    val notes: LiveData<String> = _notes
    
    // UI state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _saveSuccess = MutableLiveData<Boolean>(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError
    
    // Data
    val allHarvesterProofs: LiveData<List<HarvesterProofEntity>>
    
    private val _proofCount = MutableLiveData<Int>(0)
    val proofCount: LiveData<Int> = _proofCount
    
    private val _unsyncedCount = MutableLiveData<Int>(0)
    val unsyncedCount: LiveData<Int> = _unsyncedCount

    init {
        val database = PalmOilDatabase.getDatabase(application)
        repository = HarvesterProofRepository(database.harvesterProofDao())
        allHarvesterProofs = repository.getAllHarvesterProofs()
        
        // Load initial counts
        loadCounts()
    }

    // Form management
    fun setTreeId(treeId: String) {
        Log.d("HarvesterProofViewModel", "Setting tree ID: $treeId")
        _treeId.value = treeId
        clearValidationError()
    }

    fun setPlotId(plotId: String) {
        Log.d("HarvesterProofViewModel", "Setting plot ID: $plotId")
        _plotId.value = plotId
        clearValidationError()
    }

    fun setImagePath(imagePath: String?) {
        Log.d("HarvesterProofViewModel", "Setting image path: $imagePath")
        _imagePath.value = imagePath
    }

    fun setNotes(notes: String) {
        Log.d("HarvesterProofViewModel", "Setting notes: $notes")
        _notes.value = notes
    }

    fun clearForm() {
        Log.d("HarvesterProofViewModel", "Clearing form")
        _treeId.value = ""
        _plotId.value = ""
        _imagePath.value = null
        _notes.value = ""
        _saveSuccess.value = false
        clearError()
        clearValidationError()
    }

    // Validation
    private fun validateForm(): String? {
        val treeIdValue = _treeId.value?.trim()
        val plotIdValue = _plotId.value?.trim()
        val imagePathValue = _imagePath.value?.trim()

        return when {
            treeIdValue.isNullOrBlank() -> "Tree ID is required"
            plotIdValue.isNullOrBlank() -> "Plot ID is required"
            imagePathValue.isNullOrBlank() -> "Image is required"
            else -> null
        }
    }

    // Save operation
    fun saveHarvesterProof() {
        Log.d("HarvesterProofViewModel", "Attempting to save harvester proof")
        
        val validationError = validateForm()
        if (validationError != null) {
            Log.w("HarvesterProofViewModel", "Validation failed: $validationError")
            _validationError.value = validationError
            return
        }

        val treeIdValue = _treeId.value?.trim() ?: return
        val plotIdValue = _plotId.value?.trim() ?: return
        val imagePathValue = _imagePath.value?.trim() ?: return
        val notesValue = _notes.value?.trim() ?: ""

        _isLoading.value = true
        clearError()

        viewModelScope.launch {
            try {
                Log.d("HarvesterProofViewModel", "Creating harvester proof with treeId: $treeIdValue, plotId: $plotIdValue, imagePath: $imagePathValue")
                
                val proofId = repository.createHarvesterProof(
                    treeId = treeIdValue,
                    plotId = plotIdValue,
                    imagePath = imagePathValue,
                    notes = notesValue.ifBlank { null }
                )
                
                Log.d("HarvesterProofViewModel", "Harvester proof saved successfully with ID: $proofId")
                _saveSuccess.value = true
                loadCounts() // Refresh counts
                
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error saving harvester proof", e)
                _errorMessage.value = "Failed to save harvester proof: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Data operations
    fun getHarvesterProofsByTreeId(treeId: String, callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.getHarvesterProofsByTreeId(treeId)
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting proofs by tree ID", e)
                _errorMessage.value = "Failed to load proofs: ${e.message}"
            }
        }
    }

    fun getHarvesterProofsByPlotId(plotId: String, callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.getHarvesterProofsByPlotId(plotId)
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting proofs by plot ID", e)
                _errorMessage.value = "Failed to load proofs: ${e.message}"
            }
        }
    }

    fun searchHarvesterProofs(searchTerm: String, callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.searchHarvesterProofs(searchTerm)
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error searching proofs", e)
                _errorMessage.value = "Failed to search proofs: ${e.message}"
            }
        }
    }

    fun getRecentProofs(callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.getRecentHarvesterProofs()
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting recent proofs", e)
                _errorMessage.value = "Failed to load recent proofs: ${e.message}"
            }
        }
    }

    // Sync operations
    fun getUnsyncedProofs(callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.getUnsyncedHarvesterProofs()
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting unsynced proofs", e)
                _errorMessage.value = "Failed to load unsynced proofs: ${e.message}"
            }
        }
    }

    fun markProofAsSynced(proofId: Long) {
        viewModelScope.launch {
            try {
                repository.markProofAsSynced(proofId)
                loadCounts() // Refresh counts
                Log.d("HarvesterProofViewModel", "Proof $proofId marked as synced")
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error marking proof as synced", e)
                _errorMessage.value = "Failed to update sync status: ${e.message}"
            }
        }
    }

    // Statistics
    private fun loadCounts() {
        viewModelScope.launch {
            try {
                val totalCount = repository.getProofsCount()
                val unsyncedCount = repository.getUnsyncedProofsCount()
                
                _proofCount.value = totalCount
                _unsyncedCount.value = unsyncedCount
                
                Log.d("HarvesterProofViewModel", "Counts loaded - Total: $totalCount, Unsynced: $unsyncedCount")
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error loading counts", e)
            }
        }
    }

    // Gallery operations
    fun getProofsWithImages(callback: (List<HarvesterProofEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val proofs = repository.getHarvesterProofsWithImages()
                callback(proofs)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting proofs with images", e)
                _errorMessage.value = "Failed to load images: ${e.message}"
            }
        }
    }

    fun getAllImagePaths(callback: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val imagePaths = repository.getAllImagePaths()
                callback(imagePaths)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error getting image paths", e)
                _errorMessage.value = "Failed to load image paths: ${e.message}"
            }
        }
    }

    // Delete operations
    fun deleteHarvesterProof(proof: HarvesterProofEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHarvesterProof(proof)
                loadCounts() // Refresh counts
                Log.d("HarvesterProofViewModel", "Harvester proof deleted: ${proof.id}")
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error deleting harvester proof", e)
                _errorMessage.value = "Failed to delete proof: ${e.message}"
            }
        }
    }

    // Error handling
    fun clearError() {
        _errorMessage.value = null
    }

    private fun clearValidationError() {
        _validationError.value = null
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    // Utility methods
    fun isTreeIdAndPlotIdExists(treeId: String, plotId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val exists = repository.isTreeIdAndPlotIdExists(treeId, plotId)
                callback(exists)
            } catch (e: Exception) {
                Log.e("HarvesterProofViewModel", "Error checking if tree ID and plot ID exists", e)
                callback(false)
            }
        }
    }

    fun refreshData() {
        loadCounts()
    }
}
