package com.example.palm_oil.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HarvesterProofDao {
    
    // Basic CRUD Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHarvesterProof(harvesterProof: HarvesterProofEntity): Long

    @Update
    suspend fun updateHarvesterProof(harvesterProof: HarvesterProofEntity)

    @Delete
    suspend fun deleteHarvesterProof(harvesterProof: HarvesterProofEntity)

    // Get all proofs ordered by creation date (newest first)
    @Query("SELECT * FROM harvester_proofs ORDER BY created_at DESC")
    fun getAllHarvesterProofs(): LiveData<List<HarvesterProofEntity>>

    // Get all proofs as list (for sync operations)
    @Query("SELECT * FROM harvester_proofs ORDER BY created_at DESC")
    suspend fun getAllHarvesterProofsSync(): List<HarvesterProofEntity>

    // Get proof by ID
    @Query("SELECT * FROM harvester_proofs WHERE id = :id")
    suspend fun getHarvesterProofById(id: Long): HarvesterProofEntity?

    // Get proofs by tree ID
    @Query("SELECT * FROM harvester_proofs WHERE tree_id = :treeId ORDER BY created_at DESC")
    suspend fun getHarvesterProofsByTreeId(treeId: String): List<HarvesterProofEntity>

    // Get proofs by plot ID
    @Query("SELECT * FROM harvester_proofs WHERE plot_id = :plotId ORDER BY created_at DESC")
    suspend fun getHarvesterProofsByPlotId(plotId: String): List<HarvesterProofEntity>

    // Get proofs by tree ID and plot ID
    @Query("SELECT * FROM harvester_proofs WHERE tree_id = :treeId AND plot_id = :plotId ORDER BY created_at DESC")
    suspend fun getHarvesterProofsByTreeAndPlot(treeId: String, plotId: String): List<HarvesterProofEntity>

    // Get proofs by date range
    @Query("SELECT * FROM harvester_proofs WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    suspend fun getHarvesterProofsByDateRange(startDate: Long, endDate: Long): List<HarvesterProofEntity>

    // Get unsynced proofs
    @Query("SELECT * FROM harvester_proofs WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedHarvesterProofs(): List<HarvesterProofEntity>

    // Mark proof as synced
    @Query("UPDATE harvester_proofs SET is_synced = 1, sync_timestamp = :syncTimestamp WHERE id = :id")
    suspend fun markProofAsSynced(id: Long, syncTimestamp: Long = System.currentTimeMillis())

    // Mark all proofs as synced
    @Query("UPDATE harvester_proofs SET is_synced = 1, sync_timestamp = :syncTimestamp")
    suspend fun markAllProofsAsSynced(syncTimestamp: Long = System.currentTimeMillis())

    // Get count of all proofs
    @Query("SELECT COUNT(*) FROM harvester_proofs")
    suspend fun getProofsCount(): Int

    // Get count of unsynced proofs
    @Query("SELECT COUNT(*) FROM harvester_proofs WHERE is_synced = 0")
    suspend fun getUnsyncedProofsCount(): Int

    // Get count of proofs by tree ID
    @Query("SELECT COUNT(*) FROM harvester_proofs WHERE tree_id = :treeId")
    suspend fun getProofsCountByTreeId(treeId: String): Int

    // Delete proof by ID
    @Query("DELETE FROM harvester_proofs WHERE id = :id")
    suspend fun deleteHarvesterProofById(id: Long)

    // Delete all proofs
    @Query("DELETE FROM harvester_proofs")
    suspend fun deleteAllHarvesterProofs()

    // Delete synced proofs (cleanup)
    @Query("DELETE FROM harvester_proofs WHERE is_synced = 1")
    suspend fun deleteSyncedProofs()

    // Search proofs by tree ID pattern
    @Query("SELECT * FROM harvester_proofs WHERE tree_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchHarvesterProofsByTreeId(searchTerm: String): List<HarvesterProofEntity>

    // Search proofs by plot ID pattern
    @Query("SELECT * FROM harvester_proofs WHERE plot_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchHarvesterProofsByPlotId(searchTerm: String): List<HarvesterProofEntity>

    // Search proofs by tree ID or plot ID pattern
    @Query("SELECT * FROM harvester_proofs WHERE tree_id LIKE '%' || :searchTerm || '%' OR plot_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchHarvesterProofs(searchTerm: String): List<HarvesterProofEntity>

    // Get recent proofs (last 7 days)
    @Query("SELECT * FROM harvester_proofs WHERE created_at >= :weekAgo ORDER BY created_at DESC")
    suspend fun getRecentHarvesterProofs(weekAgo: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)): List<HarvesterProofEntity>

    // Get all image paths (for gallery)
    @Query("SELECT image_path FROM harvester_proofs WHERE image_path IS NOT NULL ORDER BY created_at DESC")
    suspend fun getAllImagePaths(): List<String>

    // Get proofs with images
    @Query("SELECT * FROM harvester_proofs WHERE image_path IS NOT NULL ORDER BY created_at DESC")
    suspend fun getHarvesterProofsWithImages(): List<HarvesterProofEntity>
}
