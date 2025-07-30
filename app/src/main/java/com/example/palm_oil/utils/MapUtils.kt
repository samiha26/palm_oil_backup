package com.example.palm_oil.utils

import com.example.palm_oil.data.model.TreeLocation
import kotlin.math.*

object MapUtils {
    
    /**
     * Calculate distance between two points in map coordinates
     */
    fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
    }
    
    /**
     * Calculate distance between two tree locations
     */
    fun calculateDistance(tree1: TreeLocation, tree2: TreeLocation): Float {
        return calculateDistance(tree1.xCoordinate, tree1.yCoordinate, tree2.xCoordinate, tree2.yCoordinate)
    }
    
    /**
     * Find the closest tree to a given point
     */
    fun findClosestTree(x: Float, y: Float, trees: List<TreeLocation>): TreeLocation? {
        return trees.minByOrNull { calculateDistance(x, y, it.xCoordinate, it.yCoordinate) }
    }
    
    /**
     * Calculate the optimal path for harvesting (simple greedy algorithm)
     * This is a basic implementation - could be improved with more sophisticated algorithms
     */
    fun calculateOptimalPath(startX: Float, startY: Float, trees: List<TreeLocation>): List<TreeLocation> {
        if (trees.isEmpty()) return emptyList()
        
        val path = mutableListOf<TreeLocation>()
        val unvisited = trees.toMutableSet()
        var currentX = startX
        var currentY = startY
        
        while (unvisited.isNotEmpty()) {
            val closest = unvisited.minByOrNull { 
                calculateDistance(currentX, currentY, it.xCoordinate, it.yCoordinate) 
            }
            
            closest?.let {
                path.add(it)
                unvisited.remove(it)
                currentX = it.xCoordinate
                currentY = it.yCoordinate
            }
        }
        
        return path
    }
    
    /**
     * Calculate total path distance
     */
    fun calculatePathDistance(path: List<TreeLocation>): Float {
        if (path.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 0 until path.size - 1) {
            totalDistance += calculateDistance(path[i], path[i + 1])
        }
        return totalDistance
    }
    
    /**
     * Convert GPS coordinates to map coordinates (if available)
     * This is a placeholder - would need actual coordinate system conversion
     */
    fun gpsToMapCoordinates(latitude: Double, longitude: Double, plotBounds: FloatArray): Pair<Float, Float> {
        // This is a simplified conversion - in real implementation, you'd need proper coordinate transformation
        // For now, we'll use a basic linear mapping
        val x = ((longitude - plotBounds[0]) / (plotBounds[2] - plotBounds[0]) * 2000f).toFloat()
        val y = ((latitude - plotBounds[1]) / (plotBounds[3] - plotBounds[1]) * 2000f).toFloat()
        return Pair(x, y)
    }
    
    /**
     * Convert map coordinates to GPS coordinates (if reference points available)
     */
    fun mapToGpsCoordinates(x: Float, y: Float, plotBounds: FloatArray): Pair<Double, Double> {
        // Reverse of the above conversion
        val longitude = plotBounds[0] + (x / 2000f) * (plotBounds[2] - plotBounds[0])
        val latitude = plotBounds[1] + (y / 2000f) * (plotBounds[3] - plotBounds[1])
        return Pair(latitude.toDouble(), longitude.toDouble())
    }
    
    /**
     * Generate grid reference for a given coordinate (e.g., A1, B5, etc.)
     */
    fun coordinateToGridReference(x: Float, y: Float, gridSize: Float = 50f): String {
        val col = (x / gridSize).toInt()
        val row = (y / gridSize).toInt()
        
        val colLetter = if (col < 26) {
            ('A' + col).toString()
        } else {
            ('A' + (col / 26 - 1)).toString() + ('A' + (col % 26)).toString()
        }
        
        return "$colLetter${row + 1}"
    }
    
    /**
     * Convert grid reference back to coordinates
     */
    fun gridReferenceToCoordinate(gridRef: String, gridSize: Float = 50f): Pair<Float, Float> {
        val letters = gridRef.takeWhile { it.isLetter() }
        val numbers = gridRef.dropWhile { it.isLetter() }
        
        val col = if (letters.length == 1) {
            letters[0] - 'A'
        } else {
            (letters[0] - 'A' + 1) * 26 + (letters[1] - 'A')
        }
        
        val row = numbers.toIntOrNull()?.minus(1) ?: 0
        
        return Pair(col * gridSize, row * gridSize)
    }
    
    /**
     * Calculate tree density in a given area
     */
    fun calculateTreeDensity(trees: List<TreeLocation>, areaX: Float, areaY: Float, areaWidth: Float, areaHeight: Float): Float {
        val treesInArea = trees.count { tree ->
            tree.xCoordinate >= areaX && tree.xCoordinate <= areaX + areaWidth &&
            tree.yCoordinate >= areaY && tree.yCoordinate <= areaY + areaHeight
        }
        
        val areaSize = areaWidth * areaHeight
        return if (areaSize > 0) treesInArea / areaSize else 0f
    }
    
    /**
     * Find trees within a radius of a given point
     */
    fun findTreesInRadius(centerX: Float, centerY: Float, radius: Float, trees: List<TreeLocation>): List<TreeLocation> {
        return trees.filter { tree ->
            calculateDistance(centerX, centerY, tree.xCoordinate, tree.yCoordinate) <= radius
        }
    }
}
