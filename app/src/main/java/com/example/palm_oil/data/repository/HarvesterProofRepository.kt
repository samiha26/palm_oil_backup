package com.example.palm_oil.data.repository

import androidx.lifecycle.LiveData
import com.example.palm_oil.data.database.HarvesterProofDao
import com.example.palm_oil.data.database.HarvesterProofEntity

class HarvesterProofRepository(
    private val harvesterProofDao: HarvesterProofDao
) {
    
    // Basic CRUD operations
    suspend fun insertHarvesterProof(harvesterProof: HarvesterProofEntity): Long {
        return harvesterProofDao.insertHarvesterProof(harvesterProof)
    }

    suspend fun updateHarvesterProof(harvesterProof: HarvesterProofEntity) {
        harvesterProofDao.updateHarvesterProof(harvesterProof)
    }

    suspend fun deleteHarvesterProof(harvesterProof: HarvesterProofEntity) {
        harvesterProofDao.deleteHarvesterProof(harvesterProof)
    }

    suspend fun deleteHarvesterProofById(id: Long) {
        harvesterProofDao.deleteHarvesterProofById(id)
    }

    // Query operations
    fun getAllHarvesterProofs(): LiveData<List<HarvesterProofEntity>> {
        return harvesterProofDao.getAllHarvesterProofs()
    }

    suspend fun getAllHarvesterProofsSync(): List<HarvesterProofEntity> {
        return harvesterProofDao.getAllHarvesterProofsSync()
    }

    suspend fun getHarvesterProofById(id: Long): HarvesterProofEntity? {
        return harvesterProofDao.getHarvesterProofById(id)
    }

    suspend fun getHarvesterProofsByTreeId(treeId: String): List<HarvesterProofEntity> {
        return harvesterProofDao.getHarvesterProofsByTreeId(treeId)
    }

    suspend fun getHarvesterProofsByPlotId(plotId: String): List<HarvesterProofEntity> {
        return harvesterProofDao.getHarvesterProofsByPlotId(plotId)
    }

    suspend fun getHarvesterProofsByTreeAndPlot(treeId: String, plotId: String): List<HarvesterProofEntity> {
        return harvesterProofDao.getHarvesterProofsByTreeAndPlot(treeId, plotId)
    }

    suspend fun getHarvesterProofsByDateRange(startDate: Long, endDate: Long): List<HarvesterProofEntity> {
        return harvesterProofDao.getHarvesterProofsByDateRange(startDate, endDate)
    }

    // Sync operations
    suspend fun getUnsyncedHarvesterProofs(): List<HarvesterProofEntity> {
        return harvesterProofDao.getUnsyncedHarvesterProofs()
    }

    suspend fun markProofAsSynced(id: Long, syncTimestamp: Long = System.currentTimeMillis()) {
        harvesterProofDao.markProofAsSynced(id, syncTimestamp)
    }

    suspend fun markAllProofsAsSynced(syncTimestamp: Long = System.currentTimeMillis()) {
        harvesterProofDao.markAllProofsAsSynced(syncTimestamp)
    }

    // Statistics and counts
    suspend fun getProofsCount(): Int {
        return harvesterProofDao.getProofsCount()
    }

    suspend fun getUnsyncedProofsCount(): Int {
        return harvesterProofDao.getUnsyncedProofsCount()
    }

    suspend fun getProofsCountByTreeId(treeId: String): Int {
        return harvesterProofDao.getProofsCountByTreeId(treeId)
    }

    // Search operations
    suspend fun searchHarvesterProofsByTreeId(searchTerm: String): List<HarvesterProofEntity> {
        return harvesterProofDao.searchHarvesterProofsByTreeId(searchTerm)
    }

    suspend fun searchHarvesterProofsByPlotId(searchTerm: String): List<HarvesterProofEntity> {
        return harvesterProofDao.searchHarvesterProofsByPlotId(searchTerm)
    }

    suspend fun searchHarvesterProofs(searchTerm: String): List<HarvesterProofEntity> {
        return harvesterProofDao.searchHarvesterProofs(searchTerm)
    }

    suspend fun getRecentHarvesterProofs(weekAgo: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)): List<HarvesterProofEntity> {
        return harvesterProofDao.getRecentHarvesterProofs(weekAgo)
    }

    // Image operations
    suspend fun getHarvesterProofsWithImages(): List<HarvesterProofEntity> {
        return harvesterProofDao.getHarvesterProofsWithImages()
    }

    suspend fun getAllImagePaths(): List<String> {
        return harvesterProofDao.getAllImagePaths()
    }

    // Cleanup operations
    suspend fun deleteAllHarvesterProofs() {
        harvesterProofDao.deleteAllHarvesterProofs()
    }

    suspend fun deleteSyncedProofs() {
        harvesterProofDao.deleteSyncedProofs()
    }

    // Convenience methods
    suspend fun createHarvesterProof(
        treeId: String,
        plotId: String,
        imagePath: String,
        latitude: Double? = null,
        longitude: Double? = null,
        notes: String? = null
    ): Long {
        val currentTime = System.currentTimeMillis()
        val harvesterProof = HarvesterProofEntity(
            treeId = treeId,
            plotId = plotId,
            imagePath = imagePath,
            createdAt = currentTime,
            updatedAt = currentTime,
            locationLatitude = latitude,
            locationLongitude = longitude,
            notes = notes
        )
        return insertHarvesterProof(harvesterProof)
    }

    suspend fun updateHarvesterProofNotes(id: Long, notes: String) {
        val proof = getHarvesterProofById(id)
        proof?.let {
            val updatedProof = it.copy(
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            updateHarvesterProof(updatedProof)
        }
    }

    suspend fun isTreeIdAndPlotIdExists(treeId: String, plotId: String): Boolean {
        return getHarvesterProofsByTreeAndPlot(treeId, plotId).isNotEmpty()
    }
}
