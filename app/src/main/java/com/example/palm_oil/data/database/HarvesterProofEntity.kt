package com.example.palm_oil.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvester_proofs",
    indices = [
        Index(value = ["tree_id"], unique = false),
        Index(value = ["plot_id"], unique = false),
        Index(value = ["created_at"], unique = false),
        Index(value = ["is_synced"], unique = false)
    ]
)
data class HarvesterProofEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "tree_id")
    val treeId: String,
    
    @ColumnInfo(name = "plot_id")
    val plotId: String,
    
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "sync_timestamp")
    val syncTimestamp: Long? = null,
    
    @ColumnInfo(name = "location_latitude")
    val locationLatitude: Double? = null,
    
    @ColumnInfo(name = "location_longitude")
    val locationLongitude: Double? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
