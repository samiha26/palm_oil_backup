package com.example.palm_oil.ui.harvestermap

import android.content.Context
import android.graphics.*
import android.location.Location
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.palm_oil.R
import com.example.palm_oil.data.database.TreeLocationEntity
import kotlin.math.max
import kotlin.math.min

class HarvesterMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Map properties
    private var scaleFactor = 1.0f
    private var translateX = 0f
    private var translateY = 0f
    
    // Map dimensions
    private val mapWidth = 2000f
    private val mapHeight = 2000f
    private val gridSize = 50f
    
    // Data
    private var treesToHarvest = listOf<TreeLocationEntity>()
    private var harvestPath = listOf<TreeLocationEntity>()
    private var userLocation: Location? = null
    private var userMapLocation: Pair<Float, Float>? = null
    
    // Paint objects
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
        alpha = 100
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    
    private val harvestTreePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.teal_700)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val pathLinePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val pathArrowPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val treeTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val userLocationPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val userLocationBorderPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    // Gesture detectors
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    init {
        // Set up initial translation to center the map
        post {
            translateX = (width - mapWidth) / 2f
            translateY = (height - mapHeight) / 2f
            invalidate()
        }
    }
    
    fun setTreesToHarvest(trees: List<TreeLocationEntity>) {
        treesToHarvest = trees
        invalidate()
    }
    
    fun setHarvestPath(path: List<TreeLocationEntity>) {
        harvestPath = path
        invalidate()
    }
    
    fun setUserLocation(location: Location?) {
        userLocation = location
        // Convert GPS to map coordinates
        location?.let {
            // For now, use center of map. In real implementation, 
            // you'd use proper coordinate transformation
            userMapLocation = Pair(1000f, 1000f)
        }
        invalidate()
    }
    
    fun resetView() {
        scaleFactor = 1.0f
        translateX = (width - mapWidth) / 2f
        translateY = (height - mapHeight) / 2f
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)
        
        // Draw background
        canvas.drawRect(0f, 0f, mapWidth, mapHeight, backgroundPaint)
        
        // Draw grid
        drawGrid(canvas)
        
        // Draw border
        canvas.drawRect(0f, 0f, mapWidth, mapHeight, borderPaint)
        
        // Draw harvest path lines
        drawHarvestPath(canvas)
        
        // Draw trees to harvest
        drawHarvestTrees(canvas)
        
        // Draw user location
        drawUserLocation(canvas)
        
        canvas.restore()
        
        // Draw info
        drawMapInfo(canvas)
    }
    
    private fun drawGrid(canvas: Canvas) {
        // Draw vertical lines
        var x = 0f
        while (x <= mapWidth) {
            canvas.drawLine(x, 0f, x, mapHeight, gridPaint)
            x += gridSize
        }
        
        // Draw horizontal lines
        var y = 0f
        while (y <= mapHeight) {
            canvas.drawLine(0f, y, mapWidth, y, gridPaint)
            y += gridSize
        }
    }
    
    private fun drawHarvestTrees(canvas: Canvas) {
        val treeRadius = 25f / scaleFactor
        
        treesToHarvest.forEachIndexed { _, tree ->
            // Draw tree circle
            canvas.drawCircle(tree.xCoordinate, tree.yCoordinate, treeRadius, harvestTreePaint)
            
            // Draw tree ID
            if (scaleFactor > 0.5f) {
                val textSize = 20f / scaleFactor
                treeTextPaint.textSize = textSize
                
                canvas.drawText(
                    tree.treeId,
                    tree.xCoordinate,
                    tree.yCoordinate + textSize / 3f,
                    treeTextPaint
                )
            }
            
            // Draw harvest order number
            if (harvestPath.isNotEmpty()) {
                val pathIndex = harvestPath.indexOf(tree)
                if (pathIndex >= 0) {
                    val orderTextPaint = Paint().apply {
                        color = Color.YELLOW
                        textSize = 16f / scaleFactor
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    
                    // Draw order number above tree
                    canvas.drawText(
                        (pathIndex + 1).toString(),
                        tree.xCoordinate,
                        tree.yCoordinate - treeRadius - 10f / scaleFactor,
                        orderTextPaint
                    )
                }
            }
        }
    }
    
    private fun drawHarvestPath(canvas: Canvas) {
        if (harvestPath.size < 2) return
        
        // Start from user location if available
        userMapLocation?.let { (userX, userY) ->
            if (harvestPath.isNotEmpty()) {
                val firstTree = harvestPath[0]
                canvas.drawLine(userX, userY, firstTree.xCoordinate, firstTree.yCoordinate, pathLinePaint)
                drawArrow(canvas, userX, userY, firstTree.xCoordinate, firstTree.yCoordinate)
            }
        }
        
        // Draw path between trees
        for (i in 0 until harvestPath.size - 1) {
            val currentTree = harvestPath[i]
            val nextTree = harvestPath[i + 1]
            
            // Draw line
            canvas.drawLine(
                currentTree.xCoordinate, currentTree.yCoordinate,
                nextTree.xCoordinate, nextTree.yCoordinate,
                pathLinePaint
            )
            
            // Draw arrow
            drawArrow(
                canvas,
                currentTree.xCoordinate, currentTree.yCoordinate,
                nextTree.xCoordinate, nextTree.yCoordinate
            )
        }
    }
    
    private fun drawArrow(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val arrowLength = 15f / scaleFactor
        val arrowAngle = Math.PI / 6 // 30 degrees
        
        val angle = kotlin.math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        
        // Calculate arrow head points
        val arrowX1 = endX - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
        val arrowY1 = endY - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
        val arrowX2 = endX - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
        val arrowY2 = endY - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()
        
        // Draw arrow head
        canvas.drawLine(endX, endY, arrowX1, arrowY1, pathArrowPaint)
        canvas.drawLine(endX, endY, arrowX2, arrowY2, pathArrowPaint)
    }
    
    private fun drawUserLocation(canvas: Canvas) {
        userMapLocation?.let { (x, y) ->
            val radius = 20f / scaleFactor
            
            // Draw user location marker
            canvas.drawCircle(x, y, radius, userLocationPaint)
            canvas.drawCircle(x, y, radius, userLocationBorderPaint)
            
            // Draw direction indicator (small arrow pointing up)
            val arrowSize = 10f / scaleFactor
            canvas.drawLine(x, y - radius - arrowSize, x, y - radius - arrowSize * 2, userLocationBorderPaint)
            canvas.drawLine(x, y - radius - arrowSize * 2, x - arrowSize/2, y - radius - arrowSize * 1.5f, userLocationBorderPaint)
            canvas.drawLine(x, y - radius - arrowSize * 2, x + arrowSize/2, y - radius - arrowSize * 1.5f, userLocationBorderPaint)
        }
    }
    
    private fun drawMapInfo(canvas: Canvas) {
        val infoPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        val info = "Trees to harvest: ${treesToHarvest.size} | Scale: ${String.format("%.1f", scaleFactor)}x"
        canvas.drawText(info, 20f, height - 20f, infoPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - event.x
                    val dy = event.y - event.y
                    
                    translateX += dx
                    translateY += dy
                    
                    constrainTranslation()
                    invalidate()
                }
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun constrainTranslation() {
        val scaledWidth = mapWidth * scaleFactor
        val scaledHeight = mapHeight * scaleFactor
        
        val minTranslateX = width - scaledWidth
        val minTranslateY = height - scaledHeight
        
        translateX = translateX.coerceIn(minTranslateX, 0f)
        translateY = translateY.coerceIn(minTranslateY, 0f)
    }
    
    private fun getMapCoordinates(screenX: Float, screenY: Float): Pair<Float, Float> {
        val mapX = (screenX - translateX) / scaleFactor
        val mapY = (screenY - translateY) / scaleFactor
        return Pair(mapX, mapY)
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.3f, min(scaleFactor, 5.0f))
            
            // Adjust translation to scale around the focal point
            val focusX = detector.focusX
            val focusY = detector.focusY
            
            translateX = focusX - (focusX - translateX) * detector.scaleFactor
            translateY = focusY - (focusY - translateY) * detector.scaleFactor
            
            constrainTranslation()
            invalidate()
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Reset zoom and center the map
            scaleFactor = 1.0f
            translateX = (width - mapWidth) / 2f
            translateY = (height - mapHeight) / 2f
            invalidate()
            return true
        }
    }
}
