package com.example.palm_oil.data.repository

import androidx.lifecycle.LiveData
import com.example.palm_oil.data.database.ReconFormDao
import com.example.palm_oil.data.database.ReconFormEntity
import com.example.palm_oil.data.database.DatabaseStats
import com.example.palm_oil.data.database.DatabaseUtils

class ReconFormRepository(private val reconFormDao: ReconFormDao) {

    // LiveData for reactive UI updates
    fun getAllReconForms(): LiveData<List<ReconFormEntity>> {
        return reconFormDao.getAllReconForms()
    }

    // CRUD Operations
    suspend fun insertReconForm(reconForm: ReconFormEntity): Long {
        return reconFormDao.insertReconForm(reconForm)
    }

    suspend fun updateReconForm(reconForm: ReconFormEntity) {
        reconFormDao.updateReconForm(reconForm)
    }

    suspend fun deleteReconForm(reconForm: ReconFormEntity) {
        reconFormDao.deleteReconForm(reconForm)
    }

    suspend fun deleteReconFormById(id: Long) {
        reconFormDao.deleteReconFormById(id)
    }

    // Query Operations
    suspend fun getReconFormById(id: Long): ReconFormEntity? {
        return reconFormDao.getReconFormById(id)
    }

    suspend fun getReconFormsByTreeId(treeId: String): List<ReconFormEntity> {
        return reconFormDao.getReconFormsByTreeId(treeId)
    }

    suspend fun getReconFormsByPlotId(plotId: String): List<ReconFormEntity> {
        return reconFormDao.getReconFormsByPlotId(plotId)
    }

    suspend fun getReconFormsByDateRange(startDate: Long, endDate: Long): List<ReconFormEntity> {
        return reconFormDao.getReconFormsByDateRange(startDate, endDate)
    }

    suspend fun getReconFormsByHarvestDays(harvestDays: Int): List<ReconFormEntity> {
        return reconFormDao.getReconFormsByHarvestDays(harvestDays)
    }

    // Search Operations
    suspend fun searchReconFormsByTreeId(searchTerm: String): List<ReconFormEntity> {
        return reconFormDao.searchReconFormsByTreeId(searchTerm)
    }

    suspend fun searchReconFormsByPlotId(searchTerm: String): List<ReconFormEntity> {
        return reconFormDao.searchReconFormsByPlotId(searchTerm)
    }

    // Sync Operations
    suspend fun getUnsyncedReconForms(): List<ReconFormEntity> {
        return reconFormDao.getUnsyncedReconForms()
    }

    suspend fun markFormAsSynced(id: Long) {
        reconFormDao.markFormAsSynced(id)
    }

    suspend fun markAllFormsAsSynced() {
        reconFormDao.markAllFormsAsSynced()
    }

    // Statistics
    suspend fun getFormsCount(): Int {
        return reconFormDao.getFormsCount()
    }

    suspend fun getUnsyncedFormsCount(): Int {
        return reconFormDao.getUnsyncedFormsCount()
    }

    suspend fun getDatabaseStats(): DatabaseStats {
        return DatabaseUtils.getDatabaseStats(reconFormDao)
    }

    // Cleanup Operations
    suspend fun deleteAllReconForms() {
        reconFormDao.deleteAllReconForms()
    }

    suspend fun deleteSyncedForms() {
        reconFormDao.deleteSyncedForms()
    }

    // Bulk Operations
    suspend fun getAllReconFormsSync(): List<ReconFormEntity> {
        return reconFormDao.getAllReconFormsSync()
    }

    // Gallery Operations
    suspend fun getAllImagePaths(): List<String> {
        return reconFormDao.getAllImagePaths()
    }

    suspend fun getReconFormsWithImages(): List<ReconFormEntity> {
        return reconFormDao.getReconFormsWithImages()
    }
}
