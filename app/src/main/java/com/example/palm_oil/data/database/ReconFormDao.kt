package com.example.palm_oil.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReconFormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconForm(reconForm: ReconFormEntity): Long

    @Update
    suspend fun updateReconForm(reconForm: ReconFormEntity)

    @Delete
    suspend fun deleteReconForm(reconForm: ReconFormEntity)

    // Get all forms ordered by creation date (newest first)
    @Query("SELECT * FROM recon_forms ORDER BY created_at DESC")
    fun getAllReconForms(): LiveData<List<ReconFormEntity>>

    // Get all forms as list (for sync operations)
    @Query("SELECT * FROM recon_forms ORDER BY created_at DESC")
    suspend fun getAllReconFormsSync(): List<ReconFormEntity>

    // Get form by ID
    @Query("SELECT * FROM recon_forms WHERE id = :id")
    suspend fun getReconFormById(id: Long): ReconFormEntity?

    // Get forms by tree ID
    @Query("SELECT * FROM recon_forms WHERE tree_id = :treeId ORDER BY created_at DESC")
    suspend fun getReconFormsByTreeId(treeId: String): List<ReconFormEntity>

    // Get forms by plot ID
    @Query("SELECT * FROM recon_forms WHERE plot_id = :plotId ORDER BY created_at DESC")
    suspend fun getReconFormsByPlotId(plotId: String): List<ReconFormEntity>

    // Get forms by date range
    @Query("SELECT * FROM recon_forms WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    suspend fun getReconFormsByDateRange(startDate: Long, endDate: Long): List<ReconFormEntity>

    // Get forms by harvest days
    @Query("SELECT * FROM recon_forms WHERE harvest_days = :harvestDays ORDER BY created_at DESC")
    suspend fun getReconFormsByHarvestDays(harvestDays: Int): List<ReconFormEntity>

    // Get unsynced forms
    @Query("SELECT * FROM recon_forms WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedReconForms(): List<ReconFormEntity>

    // Mark form as synced
    @Query("UPDATE recon_forms SET is_synced = 1 WHERE id = :id")
    suspend fun markFormAsSynced(id: Long)

    // Mark all forms as synced
    @Query("UPDATE recon_forms SET is_synced = 1")
    suspend fun markAllFormsAsSynced()

    // Get count of all forms
    @Query("SELECT COUNT(*) FROM recon_forms")
    suspend fun getFormsCount(): Int

    // Get count of unsynced forms
    @Query("SELECT COUNT(*) FROM recon_forms WHERE is_synced = 0")
    suspend fun getUnsyncedFormsCount(): Int

    // Delete form by ID
    @Query("DELETE FROM recon_forms WHERE id = :id")
    suspend fun deleteReconFormById(id: Long)

    // Delete all forms
    @Query("DELETE FROM recon_forms")
    suspend fun deleteAllReconForms()

    // Delete synced forms (cleanup)
    @Query("DELETE FROM recon_forms WHERE is_synced = 1")
    suspend fun deleteSyncedForms()

    // Get forms with images
    @Query("SELECT * FROM recon_forms WHERE image1_path IS NOT NULL OR image2_path IS NOT NULL OR image3_path IS NOT NULL ORDER BY created_at DESC")
    suspend fun getReconFormsWithImages(): List<ReconFormEntity>

    // Search forms by tree ID pattern
    @Query("SELECT * FROM recon_forms WHERE tree_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchReconFormsByTreeId(searchTerm: String): List<ReconFormEntity>

    // Search forms by plot ID pattern
    @Query("SELECT * FROM recon_forms WHERE plot_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchReconFormsByPlotId(searchTerm: String): List<ReconFormEntity>

    // Get all image paths (for gallery)
    @Query("""
        SELECT DISTINCT image_path FROM (
            SELECT image1_path as image_path FROM recon_forms WHERE image1_path IS NOT NULL
            UNION
            SELECT image2_path as image_path FROM recon_forms WHERE image2_path IS NOT NULL
            UNION
            SELECT image3_path as image_path FROM recon_forms WHERE image3_path IS NOT NULL
        ) ORDER BY image_path
    """)
    suspend fun getAllImagePaths(): List<String>
}
