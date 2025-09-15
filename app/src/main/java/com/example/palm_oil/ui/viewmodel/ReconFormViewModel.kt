package com.example.palm_oil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.model.ReconForm
import com.example.palm_oil.data.repository.ReconFormRepository
import kotlinx.coroutines.launch

class ReconFormViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReconFormRepository
    
    // LiveData for all forms
    val allReconForms: LiveData<List<ReconFormEntity>>
    
    // LiveData for current form data
    private val _currentTreeId = MutableLiveData<String>()
    val currentTreeId: LiveData<String> = _currentTreeId
    
    private val _currentPlotId = MutableLiveData<String>()
    val currentPlotId: LiveData<String> = _currentPlotId
    
    private val _currentNumberOfFruits = MutableLiveData<Int>()
    val currentNumberOfFruits: LiveData<Int> = _currentNumberOfFruits
    
    private val _currentHarvestDays = MutableLiveData<Int>()
    val currentHarvestDays: LiveData<Int> = _currentHarvestDays
    
    private val _currentImages = MutableLiveData<MutableList<String>>()
    val currentImages: LiveData<MutableList<String>> = _currentImages
    
    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    init {
        val database = PalmOilDatabase.getDatabase(application)
        repository = ReconFormRepository(database.reconFormDao())
        allReconForms = repository.getAllReconForms()
        
        // Initialize current form data with default values
        _currentTreeId.value = ""
        _currentPlotId.value = ""
        _currentNumberOfFruits.value = 0
        _currentHarvestDays.value = 1
        _currentImages.value = mutableListOf()
    }

    fun setTreeId(treeId: String) {
        _currentTreeId.value = treeId
    }

    fun setPlotId(plotId: String) {
        _currentPlotId.value = plotId
    }

    fun setNumberOfFruits(numberOfFruits: Int) {
        _currentNumberOfFruits.value = numberOfFruits
    }

    fun setHarvestDays(harvestDays: Int) {
        _currentHarvestDays.value = harvestDays
    }

    fun addImage(imagePath: String) {
        val currentList = _currentImages.value ?: mutableListOf()
        if (currentList.size < 3) {
            currentList.add(imagePath)
            _currentImages.value = currentList
        }
    }

    fun removeImage(index: Int) {
        val currentList = _currentImages.value ?: mutableListOf()
        if (index < currentList.size) {
            currentList.removeAt(index)
            _currentImages.value = currentList
        }
    }

    fun saveReconForm() {
        val treeId = _currentTreeId.value
        val plotId = _currentPlotId.value
        val numberOfFruits = _currentNumberOfFruits.value
        val harvestDays = _currentHarvestDays.value
        val images = _currentImages.value ?: mutableListOf()

        // Debug logging
        android.util.Log.d("ReconFormViewModel", "Saving form with:")
        android.util.Log.d("ReconFormViewModel", "  treeId: '$treeId'")
        android.util.Log.d("ReconFormViewModel", "  plotId: '$plotId'")
        android.util.Log.d("ReconFormViewModel", "  numberOfFruits: $numberOfFruits")
        android.util.Log.d("ReconFormViewModel", "  harvestDays: $harvestDays")
        android.util.Log.d("ReconFormViewModel", "  images: ${images.size}")

        // Validate required fields (images are now optional)
        if (treeId.isNullOrBlank()) {
            android.util.Log.e("ReconFormViewModel", "Save failed: Tree ID is null or blank")
            _saveStatus.value = false
            return
        }

        if (plotId.isNullOrBlank()) {
            android.util.Log.e("ReconFormViewModel", "Save failed: Plot ID is null or blank")
            _saveStatus.value = false
            return
        }

        if (numberOfFruits == null || numberOfFruits <= 0) {
            android.util.Log.e("ReconFormViewModel", "Save failed: numberOfFruits is null or <= 0")
            _saveStatus.value = false
            return
        }

        android.util.Log.d("ReconFormViewModel", "All validations passed, creating ReconFormEntity")

        val reconForm = ReconFormEntity(
            treeId = treeId,
            plotId = plotId,
            numberOfFruits = numberOfFruits,
            harvestDays = harvestDays ?: 1,
            image1Path = images.getOrNull(0),
            image2Path = images.getOrNull(1),
            image3Path = images.getOrNull(2)
        )

        viewModelScope.launch {
            try {
                android.util.Log.d("ReconFormViewModel", "Attempting to insert recon form into database")
                repository.insertReconForm(reconForm)
                android.util.Log.d("ReconFormViewModel", "Successfully inserted recon form (images optional)")
                _saveStatus.value = true
                clearCurrentFormExceptTreeId()
            } catch (e: Exception) {
                android.util.Log.e("ReconFormViewModel", "Failed to insert recon form: ${e.message}", e)
                _saveStatus.value = false
            }
        }
    }

    private fun clearCurrentFormExceptTreeId() {
        // Keep tree ID for next form entry, clear other fields
        _currentPlotId.value = ""
        _currentNumberOfFruits.value = 0
        _currentHarvestDays.value = 1
        _currentImages.value = mutableListOf()
    }

    fun getReconFormById(id: Long, callback: (ReconFormEntity?) -> Unit) {
        viewModelScope.launch {
            val result = repository.getReconFormById(id)
            callback(result)
        }
    }

    fun deleteReconForm(id: Long) {
        viewModelScope.launch {
            repository.deleteReconFormById(id)
        }
    }
}
