package com.example.palm_oil.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TreeLocationDao {
    
    // Basic CRUD Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeLocation(treeLocation: TreeLocationEntity): Long

    @Update
    suspend fun updateTreeLocation(treeLocation: TreeLocationEntity)

    @Delete
    suspend fun deleteTreeLocation(treeLocation: TreeLocationEntity)

    // Get all tree locations ordered by creation date (newest first)
    @Query("SELECT * FROM tree_locations ORDER BY created_at DESC")
    fun getAllTreeLocations(): LiveData<List<TreeLocationEntity>>

    // Get all tree locations as list (for sync operations)
    @Query("SELECT * FROM tree_locations ORDER BY created_at DESC")
    suspend fun getAllTreeLocationsSync(): List<TreeLocationEntity>

    // Get tree location by ID
    @Query("SELECT * FROM tree_locations WHERE id = :id")
    suspend fun getTreeLocationById(id: Long): TreeLocationEntity?

    // Get tree locations by plot ID
    @Query("SELECT * FROM tree_locations WHERE plot_id = :plotId ORDER BY tree_id ASC")
    suspend fun getTreeLocationsByPlotId(plotId: String): List<TreeLocationEntity>

    // Get tree locations by plot ID as LiveData
    @Query("SELECT * FROM tree_locations WHERE plot_id = :plotId ORDER BY tree_id ASC")
    fun getTreeLocationsByPlotIdLiveData(plotId: String): LiveData<List<TreeLocationEntity>>

    // Get tree location by tree ID and plot ID
    @Query("SELECT * FROM tree_locations WHERE tree_id = :treeId AND plot_id = :plotId")
    suspend fun getTreeLocationByTreeAndPlot(treeId: String, plotId: String): TreeLocationEntity?

    // Check if tree exists in plot
    @Query("SELECT COUNT(*) > 0 FROM tree_locations WHERE tree_id = :treeId AND plot_id = :plotId")
    suspend fun isTreeExistsInPlot(treeId: String, plotId: String): Boolean

    // Get tree locations by date range
    @Query("SELECT * FROM tree_locations WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    suspend fun getTreeLocationsByDateRange(startDate: Long, endDate: Long): List<TreeLocationEntity>

    // Get unsynced tree locations
    @Query("SELECT * FROM tree_locations WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedTreeLocations(): List<TreeLocationEntity>

    // Mark tree location as synced
    @Query("UPDATE tree_locations SET is_synced = 1, sync_timestamp = :syncTimestamp WHERE id = :id")
    suspend fun markTreeLocationAsSynced(id: Long, syncTimestamp: Long = System.currentTimeMillis())

    // Mark all tree locations as synced
    @Query("UPDATE tree_locations SET is_synced = 1, sync_timestamp = :syncTimestamp")
    suspend fun markAllTreeLocationsAsSynced(syncTimestamp: Long = System.currentTimeMillis())

    // Get count of all tree locations
    @Query("SELECT COUNT(*) FROM tree_locations")
    suspend fun getTreeLocationsCount(): Int

    // Get count of unsynced tree locations
    @Query("SELECT COUNT(*) FROM tree_locations WHERE is_synced = 0")
    suspend fun getUnsyncedTreeLocationsCount(): Int

    // Get count of tree locations by plot ID
    @Query("SELECT COUNT(*) FROM tree_locations WHERE plot_id = :plotId")
    suspend fun getTreeLocationsCountByPlotId(plotId: String): Int

    // Delete tree location by ID
    @Query("DELETE FROM tree_locations WHERE id = :id")
    suspend fun deleteTreeLocationById(id: Long)

    // Delete all tree locations
    @Query("DELETE FROM tree_locations")
    suspend fun deleteAllTreeLocations()

    // Delete synced tree locations (cleanup)
    @Query("DELETE FROM tree_locations WHERE is_synced = 1")
    suspend fun deleteSyncedTreeLocations()

    // Search tree locations by tree ID pattern
    @Query("SELECT * FROM tree_locations WHERE tree_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchTreeLocationsByTreeId(searchTerm: String): List<TreeLocationEntity>

    // Search tree locations by plot ID pattern
    @Query("SELECT * FROM tree_locations WHERE plot_id LIKE '%' || :searchTerm || '%' ORDER BY created_at DESC")
    suspend fun searchTreeLocationsByPlotId(searchTerm: String): List<TreeLocationEntity>

    // Get distinct plot IDs that have tree locations
    @Query("SELECT DISTINCT plot_id FROM tree_locations ORDER BY plot_id ASC")
    suspend fun getDistinctPlotIds(): List<String>

    // Get tree locations with GPS coordinates
    @Query("SELECT * FROM tree_locations WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY created_at DESC")
    suspend fun getTreeLocationsWithGPS(): List<TreeLocationEntity>

    // Update tree location coordinates
    @Query("UPDATE tree_locations SET x_coordinate = :x, y_coordinate = :y, updated_at = :timestamp WHERE id = :id")
    suspend fun updateTreeLocationCoordinates(id: Long, x: Float, y: Float, timestamp: Long = System.currentTimeMillis())

    // Update tree location GPS coordinates
    @Query("UPDATE tree_locations SET latitude = :latitude, longitude = :longitude, updated_at = :timestamp WHERE id = :id")
    suspend fun updateTreeLocationGPS(id: Long, latitude: Double?, longitude: Double?, timestamp: Long = System.currentTimeMillis())
}
