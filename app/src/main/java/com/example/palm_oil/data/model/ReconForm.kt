package com.example.palm_oil.data.model

data class ReconForm(
    val id: Long = 0,
    val treeId: String,
    val plotId: String,
    val numberOfFruits: Int,
    val harvestDays: Int, // 1, 2, or 3 days
    val image1Path: String? = null,
    val image2Path: String? = null,
    val image3Path: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
