package com.example.palm_oil.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recon_forms",
    indices = [
        Index(value = ["tree_id"], unique = false),
        Index(value = ["plot_id"], unique = false),
        Index(value = ["created_at"], unique = false),
        Index(value = ["is_synced"], unique = false)
    ]
)
data class ReconFormEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "tree_id")
    val treeId: String,
    
    @ColumnInfo(name = "plot_id")
    val plotId: String,
    
    @ColumnInfo(name = "number_of_fruits")
    val numberOfFruits: Int,
    
    @ColumnInfo(name = "harvest_days")
    val harvestDays: Int,
    
    @ColumnInfo(name = "image1_path")
    val image1Path: String? = null,
    
    @ColumnInfo(name = "image2_path")
    val image2Path: String? = null,
    
    @ColumnInfo(name = "image3_path")
    val image3Path: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)
