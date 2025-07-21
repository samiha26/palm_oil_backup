package com.example.palm_oil.data.model

data class TreeLocation(
    val id: Long = 0,
    val treeId: String,
    val plotId: String,
    val xCoordinate: Float,
    val yCoordinate: Float,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncTimestamp: Long? = null,
    val notes: String? = null
)
