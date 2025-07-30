package com.example.palm_oil.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tree_locations",
    indices = [
        Index(value = ["tree_id"], unique = false),
        Index(value = ["plot_id"], unique = false),
        Index(value = ["created_at"], unique = false),
        Index(value = ["is_synced"], unique = false),
        Index(value = ["plot_id", "tree_id"], unique = true) // Unique tree per plot
    ]
)
data class TreeLocationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "tree_id")
    val treeId: String,
    
    @ColumnInfo(name = "plot_id")
    val plotId: String,
    
    @ColumnInfo(name = "x_coordinate")
    val xCoordinate: Float,
    
    @ColumnInfo(name = "y_coordinate")
    val yCoordinate: Float,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "sync_timestamp")
    val syncTimestamp: Long? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
