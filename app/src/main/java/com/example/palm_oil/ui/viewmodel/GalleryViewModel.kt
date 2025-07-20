package com.example.palm_oil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.palm_oil.data.database.PalmOilDatabase
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.model.GalleryImage
import com.example.palm_oil.data.repository.ReconFormRepository
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReconFormRepository

    private val _galleryImages = MutableLiveData<List<GalleryImage>>()
    val galleryImages: LiveData<List<GalleryImage>> = _galleryImages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val database = PalmOilDatabase.getDatabase(application)
        repository = ReconFormRepository(database.reconFormDao())
        loadGalleryImages()
    }

    fun loadGalleryImages() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val formsWithImages = repository.getReconFormsWithImages()
                val galleryImages = mutableListOf<GalleryImage>()

                formsWithImages.forEach { form: ReconFormEntity ->
                    // Add image1 if exists
                    form.image1Path?.let { imagePath ->
                        galleryImages.add(
                            GalleryImage(
                                imagePath = imagePath,
                                formId = form.id,
                                treeId = form.treeId,
                                plotId = form.plotId,
                                createdAt = form.createdAt,
                                imageIndex = 1
                            )
                        )
                    }

                    // Add image2 if exists
                    form.image2Path?.let { imagePath ->
                        galleryImages.add(
                            GalleryImage(
                                imagePath = imagePath,
                                formId = form.id,
                                treeId = form.treeId,
                                plotId = form.plotId,
                                createdAt = form.createdAt,
                                imageIndex = 2
                            )
                        )
                    }

                    // Add image3 if exists
                    form.image3Path?.let { imagePath ->
                        galleryImages.add(
                            GalleryImage(
                                imagePath = imagePath,
                                formId = form.id,
                                treeId = form.treeId,
                                plotId = form.plotId,
                                createdAt = form.createdAt,
                                imageIndex = 3
                            )
                        )
                    }
                }

                // Sort by creation date (newest first)
                galleryImages.sortByDescending { it.createdAt }
                _galleryImages.value = galleryImages
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error loading gallery images", e)
                _galleryImages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshGallery() {
        loadGalleryImages()
    }
}
