package com.example.palm_oil.data.model

data class PlotMap(
    val plotId: String,
    val trees: List<TreeLocation>,
    val bounds: MapBounds,
    val createdAt: Long = System.currentTimeMillis()
)

data class MapBounds(
    val minX: Float = 0f,
    val minY: Float = 0f,
    val maxX: Float = 2000f,
    val maxY: Float = 2000f
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
}
