package com.example.palm_oil.data.model

data class GalleryImage(
    val imagePath: String,
    val formId: Long,
    val treeId: String,
    val plotId: String,
    val createdAt: Long,
    val imageIndex: Int // 1, 2, or 3 indicating which image slot it came from
)
