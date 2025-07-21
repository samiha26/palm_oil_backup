package com.example.palm_oil.ui.virtualmap

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.palm_oil.data.database.TreeLocationEntity
import com.example.palm_oil.data.repository.TreeLocationRepository

class VirtualMapViewModel(private val repository: TreeLocationRepository) : ViewModel() {

    fun getAllTreeLocations(): LiveData<List<TreeLocationEntity>> {
        return repository.getAllTreeLocations()
    }

    suspend fun getAllTreeLocationsSync(): List<TreeLocationEntity> {
        return repository.getAllTreeLocationsSync()
    }

    suspend fun getTreeLocationById(id: Long): TreeLocationEntity? {
        return repository.getTreeLocationById(id)
    }

    suspend fun getTreeLocationsByPlotId(plotId: String): List<TreeLocationEntity> {
        return repository.getTreeLocationsByPlotId(plotId)
    }

    fun getTreeLocationsByPlotIdLiveData(plotId: String): LiveData<List<TreeLocationEntity>> {
        return repository.getTreeLocationsByPlotIdLiveData(plotId)
    }

    suspend fun getTreeLocationByTreeAndPlot(treeId: String, plotId: String): TreeLocationEntity? {
        return repository.getTreeLocationByTreeAndPlot(treeId, plotId)
    }

    suspend fun isTreeExistsInPlot(treeId: String, plotId: String): Boolean {
        return repository.isTreeExistsInPlot(treeId, plotId)
    }

    suspend fun createTreeLocation(
        treeId: String,
        plotId: String,
        xCoordinate: Float,
        yCoordinate: Float,
        latitude: Double? = null,
        longitude: Double? = null,
        notes: String? = null
    ): Long {
        return repository.createTreeLocation(treeId, plotId, xCoordinate, yCoordinate, latitude, longitude, notes)
    }

    suspend fun updateTreeLocation(treeLocation: TreeLocationEntity) {
        repository.updateTreeLocation(treeLocation)
    }

    suspend fun deleteTreeLocation(treeLocation: TreeLocationEntity) {
        repository.deleteTreeLocation(treeLocation)
    }

    suspend fun moveTreeLocation(id: Long, newX: Float, newY: Float) {
        repository.moveTreeLocation(id, newX, newY)
    }

    suspend fun updateTreeLocationNotes(id: Long, notes: String) {
        repository.updateTreeLocationNotes(id, notes)
    }

    suspend fun getDistinctPlotIds(): List<String> {
        return repository.getDistinctPlotIds()
    }

    suspend fun getTreeLocationsCountByPlotId(plotId: String): Int {
        return repository.getTreeLocationsCountByPlotId(plotId)
    }

    suspend fun getTreeLocationsCount(): Int {
        return repository.getTreeLocationsCount()
    }

    suspend fun getUnsyncedTreeLocationsCount(): Int {
        return repository.getUnsyncedTreeLocationsCount()
    }

    suspend fun searchTreeLocationsByTreeId(searchTerm: String): List<TreeLocationEntity> {
        return repository.searchTreeLocationsByTreeId(searchTerm)
    }

    suspend fun searchTreeLocationsByPlotId(searchTerm: String): List<TreeLocationEntity> {
        return repository.searchTreeLocationsByPlotId(searchTerm)
    }

    // Sync operations
    suspend fun getUnsyncedTreeLocations(): List<TreeLocationEntity> {
        return repository.getUnsyncedTreeLocations()
    }

    suspend fun markTreeLocationAsSynced(id: Long, syncTimestamp: Long = System.currentTimeMillis()) {
        repository.markTreeLocationAsSynced(id, syncTimestamp)
    }

    suspend fun markAllTreeLocationsAsSynced(syncTimestamp: Long = System.currentTimeMillis()) {
        repository.markAllTreeLocationsAsSynced(syncTimestamp)
    }

    class Factory(private val repository: TreeLocationRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VirtualMapViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VirtualMapViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
