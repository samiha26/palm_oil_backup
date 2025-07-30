package com.example.palm_oil.data.repository

import androidx.lifecycle.LiveData
import com.example.palm_oil.data.database.TreeLocationDao
import com.example.palm_oil.data.database.TreeLocationEntity

class TreeLocationRepository(private val treeLocationDao: TreeLocationDao) {

    // Basic CRUD operations
    suspend fun insertTreeLocation(treeLocation: TreeLocationEntity): Long {
        return treeLocationDao.insertTreeLocation(treeLocation)
    }

    suspend fun updateTreeLocation(treeLocation: TreeLocationEntity) {
        treeLocationDao.updateTreeLocation(treeLocation)
    }

    suspend fun deleteTreeLocation(treeLocation: TreeLocationEntity) {
        treeLocationDao.deleteTreeLocation(treeLocation)
    }

    // Get operations
    fun getAllTreeLocations(): LiveData<List<TreeLocationEntity>> {
        return treeLocationDao.getAllTreeLocations()
    }

    suspend fun getAllTreeLocationsSync(): List<TreeLocationEntity> {
        return treeLocationDao.getAllTreeLocationsSync()
    }

    suspend fun getTreeLocationById(id: Long): TreeLocationEntity? {
        return treeLocationDao.getTreeLocationById(id)
    }

    suspend fun getTreeLocationsByPlotId(plotId: String): List<TreeLocationEntity> {
        return treeLocationDao.getTreeLocationsByPlotId(plotId)
    }

    fun getTreeLocationsByPlotIdLiveData(plotId: String): LiveData<List<TreeLocationEntity>> {
        return treeLocationDao.getTreeLocationsByPlotIdLiveData(plotId)
    }

    suspend fun getTreeLocationByTreeAndPlot(treeId: String, plotId: String): TreeLocationEntity? {
        return treeLocationDao.getTreeLocationByTreeAndPlot(treeId, plotId)
    }

    suspend fun isTreeExistsInPlot(treeId: String, plotId: String): Boolean {
        return treeLocationDao.isTreeExistsInPlot(treeId, plotId)
    }

    suspend fun getTreeLocationsByDateRange(startDate: Long, endDate: Long): List<TreeLocationEntity> {
        return treeLocationDao.getTreeLocationsByDateRange(startDate, endDate)
    }

    // Sync operations
    suspend fun getUnsyncedTreeLocations(): List<TreeLocationEntity> {
        return treeLocationDao.getUnsyncedTreeLocations()
    }

    suspend fun markTreeLocationAsSynced(id: Long, syncTimestamp: Long = System.currentTimeMillis()) {
        treeLocationDao.markTreeLocationAsSynced(id, syncTimestamp)
    }

    suspend fun markAllTreeLocationsAsSynced(syncTimestamp: Long = System.currentTimeMillis()) {
        treeLocationDao.markAllTreeLocationsAsSynced(syncTimestamp)
    }

    // Statistics and counts
    suspend fun getTreeLocationsCount(): Int {
        return treeLocationDao.getTreeLocationsCount()
    }

    suspend fun getUnsyncedTreeLocationsCount(): Int {
        return treeLocationDao.getUnsyncedTreeLocationsCount()
    }

    suspend fun getTreeLocationsCountByPlotId(plotId: String): Int {
        return treeLocationDao.getTreeLocationsCountByPlotId(plotId)
    }

    // Delete operations
    suspend fun deleteTreeLocationById(id: Long) {
        treeLocationDao.deleteTreeLocationById(id)
    }

    suspend fun deleteAllTreeLocations() {
        treeLocationDao.deleteAllTreeLocations()
    }

    suspend fun deleteSyncedTreeLocations() {
        treeLocationDao.deleteSyncedTreeLocations()
    }

    // Search operations
    suspend fun searchTreeLocationsByTreeId(searchTerm: String): List<TreeLocationEntity> {
        return treeLocationDao.searchTreeLocationsByTreeId(searchTerm)
    }

    suspend fun searchTreeLocationsByPlotId(searchTerm: String): List<TreeLocationEntity> {
        return treeLocationDao.searchTreeLocationsByPlotId(searchTerm)
    }

    suspend fun getDistinctPlotIds(): List<String> {
        return treeLocationDao.getDistinctPlotIds()
    }

    suspend fun getTreeLocationsWithGPS(): List<TreeLocationEntity> {
        return treeLocationDao.getTreeLocationsWithGPS()
    }

    // Update operations
    suspend fun updateTreeLocationCoordinates(id: Long, x: Float, y: Float) {
        treeLocationDao.updateTreeLocationCoordinates(id, x, y, System.currentTimeMillis())
    }

    suspend fun updateTreeLocationGPS(id: Long, latitude: Double?, longitude: Double?) {
        treeLocationDao.updateTreeLocationGPS(id, latitude, longitude, System.currentTimeMillis())
    }

    // Helper methods for creating tree locations
    suspend fun createTreeLocation(
        treeId: String,
        plotId: String,
        xCoordinate: Float,
        yCoordinate: Float,
        latitude: Double? = null,
        longitude: Double? = null,
        notes: String? = null
    ): Long {
        val currentTime = System.currentTimeMillis()
        val treeLocation = TreeLocationEntity(
            treeId = treeId,
            plotId = plotId,
            xCoordinate = xCoordinate,
            yCoordinate = yCoordinate,
            latitude = latitude,
            longitude = longitude,
            createdAt = currentTime,
            updatedAt = currentTime,
            notes = notes
        )
        return insertTreeLocation(treeLocation)
    }

    suspend fun updateTreeLocationNotes(id: Long, notes: String) {
        val treeLocation = getTreeLocationById(id)
        treeLocation?.let {
            val updatedTreeLocation = it.copy(
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            updateTreeLocation(updatedTreeLocation)
        }
    }

    suspend fun moveTreeLocation(id: Long, newX: Float, newY: Float) {
        val treeLocation = getTreeLocationById(id)
        treeLocation?.let {
            val updatedTreeLocation = it.copy(
                xCoordinate = newX,
                yCoordinate = newY,
                updatedAt = System.currentTimeMillis()
            )
            updateTreeLocation(updatedTreeLocation)
        }
    }
}
