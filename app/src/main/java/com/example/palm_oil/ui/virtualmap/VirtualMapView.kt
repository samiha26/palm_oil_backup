package com.example.palm_oil.ui.virtualmap

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.palm_oil.R
import com.example.palm_oil.data.database.TreeLocationEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VirtualMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Map properties
    private var scaleFactor = 1.0f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // Map dimensions
    private val mapWidth = 2000f
    private val mapHeight = 2000f
    private val gridSize = 25f // Reduced for finer resolution
    
    // Paint objects
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val treePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.teal_700)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val treeTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
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
    
    private val selectedTreePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
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
    
    private val userLocationAccuracyPaint = Paint().apply {
        color = Color.BLUE
        alpha = 50
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Tree data
    private var trees = listOf<TreeLocationEntity>()
    private var selectedTreeId: Long? = null
    
    // User location data
    private var userLocation: Pair<Float, Float>? = null
    private var userLocationAccuracy: Float = 0f
    private var plotBounds: FloatArray? = null // [minLat, minLng, maxLat, maxLng]
    
    // Gesture detectors
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    // Callbacks
    private var onTreeClickListener: ((TreeLocationEntity) -> Unit)? = null
    private var onMapClickListener: ((Float, Float) -> Unit)? = null
    
    init {
        // Set up initial translation to center the map
        post {
            translateX = (width - mapWidth) / 2f
            translateY = (height - mapHeight) / 2f
            invalidate()
        }
    }
    
    fun setTrees(newTrees: List<TreeLocationEntity>) {
        trees = newTrees
        invalidate()
    }
    
    fun setOnTreeClickListener(listener: (TreeLocationEntity) -> Unit) {
        onTreeClickListener = listener
    }
    
    fun setOnMapClickListener(listener: (Float, Float) -> Unit) {
        onMapClickListener = listener
    }
    
    fun selectTree(treeId: Long?) {
        selectedTreeId = treeId
        invalidate()
    }
    
    fun centerOnTree(treeId: Long) {
        val tree = trees.find { it.id == treeId }
        tree?.let {
            translateX = width / 2f - it.xCoordinate * scaleFactor
            translateY = height / 2f - it.yCoordinate * scaleFactor
            invalidate()
        }
    }
    
    fun resetView() {
        scaleFactor = 1.0f
        translateX = (width - mapWidth) / 2f
        translateY = (height - mapHeight) / 2f
        invalidate()
    }
    
    fun setUserLocation(latitude: Double, longitude: Double, accuracy: Float) {
        plotBounds?.let { bounds ->
            val mapCoords = gpsToMapCoordinates(latitude, longitude, bounds)
            userLocation = mapCoords
            userLocationAccuracy = accuracy
            invalidate()
        }
    }
    
    fun setPlotBounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        plotBounds = floatArrayOf(minLat.toFloat(), minLng.toFloat(), maxLat.toFloat(), maxLng.toFloat())
    }
    
    fun centerOnUserLocation() {
        userLocation?.let { (x, y) ->
            translateX = width / 2f - x * scaleFactor
            translateY = height / 2f - y * scaleFactor
            constrainTranslation()
            invalidate()
        }
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
        
        // Draw trees
        drawTrees(canvas)
        
        // Draw user location
        drawUserLocation(canvas)
        
        canvas.restore()
        
        // Draw scale and coordinates info
        drawMapInfo(canvas)
    }
    
    private fun drawGrid(canvas: Canvas) {
        // Base grid with current gridSize
        drawGridLines(canvas, gridSize, gridPaint)
        
        // Add finer grid at higher zoom levels
        if (scaleFactor > 2.0f) {
            val fineGridPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
                alpha = 100 // Semi-transparent for fine grid
            }
            drawGridLines(canvas, gridSize / 2f, fineGridPaint)
        }
        
        // Add plantation spacing guides at high zoom levels (8.5m spacing)
        if (scaleFactor > 4.0f) {
            val plantationSpacing = 34f // 8.5m = 34 pixels (8.5 * 4 pixels per meter)
            val plantationPaint = Paint().apply {
                color = Color.rgb(0, 150, 0) // Green for plantation guides
                strokeWidth = 1f
                style = Paint.Style.STROKE
                alpha = 150
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            drawGridLines(canvas, plantationSpacing, plantationPaint)
        }
    }
    
    private fun drawGridLines(canvas: Canvas, spacing: Float, paint: Paint) {
        // Draw vertical lines
        var x = 0f
        while (x <= mapWidth) {
            canvas.drawLine(x, 0f, x, mapHeight, paint)
            x += spacing
        }
        
        // Draw horizontal lines
        var y = 0f
        while (y <= mapHeight) {
            canvas.drawLine(0f, y, mapWidth, y, paint)
            y += spacing
        }
    }
    
    private fun drawTrees(canvas: Canvas) {
        // Adaptive tree size based on zoom level - better scaling for high zoom
        val baseTreeRadius = 20f
        val treeRadius = when {
            scaleFactor <= 1.0f -> baseTreeRadius / scaleFactor
            scaleFactor <= 4.0f -> baseTreeRadius / scaleFactor
            else -> max(8f, baseTreeRadius / scaleFactor) // Minimum size at very high zoom
        }
        
        trees.forEach { tree ->
            val isSelected = selectedTreeId == tree.id
            
            // Draw tree circle with appropriate paint
            val paint = if (isSelected) selectedTreePaint else treePaint
            canvas.drawCircle(tree.xCoordinate, tree.yCoordinate, treeRadius, paint)
            
            // Draw selection border if selected - adaptive border width
            if (isSelected) {
                val borderWidth = max(2f, 5f / scaleFactor)
                canvas.drawCircle(tree.xCoordinate, tree.yCoordinate, treeRadius + borderWidth, selectedTreePaint)
            }
            
            // Draw tree ID text if zoomed in enough
            if (scaleFactor > 0.8f) {
                val textSize = 24f / scaleFactor
                treeTextPaint.textSize = textSize
                
                // Draw text background circle
                val textBounds = Rect()
                treeTextPaint.getTextBounds(tree.treeId, 0, tree.treeId.length, textBounds)
                val textRadius = max(textBounds.width(), textBounds.height()) / 2f + 4f
                
                val backgroundCirclePaint = Paint().apply {
                    color = Color.BLACK
                    alpha = 128
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                
                canvas.drawCircle(tree.xCoordinate, tree.yCoordinate, textRadius, backgroundCirclePaint)
                
                // Draw tree ID
                canvas.drawText(
                    tree.treeId,
                    tree.xCoordinate,
                    tree.yCoordinate + textSize / 3f,
                    treeTextPaint
                )
            }
        }
    }
    
    private fun drawMapInfo(canvas: Canvas) {
        val infoPaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            isAntiAlias = true
        }
        
        val info = "Scale: ${String.format("%.2f", scaleFactor)}x | Trees: ${trees.size}"
        canvas.drawText(info, 20f, height - 60f, infoPaint)
        
        // Show user location info if available
        userLocation?.let {
            val locationInfo = "Your Location: (${it.first.toInt()}, ${it.second.toInt()})"
            canvas.drawText(locationInfo, 20f, height - 20f, infoPaint)
        }
    }
    
    private fun drawUserLocation(canvas: Canvas) {
        userLocation?.let { (x, y) ->
            val radius = 15f / scaleFactor
            val accuracyRadius = (userLocationAccuracy / 2f) / scaleFactor // Approximate accuracy visualization
            
            // Draw accuracy circle (if accuracy is reasonable)
            if (userLocationAccuracy < 100f && accuracyRadius > radius) {
                canvas.drawCircle(x, y, accuracyRadius, userLocationAccuracyPaint)
            }
            
            // Draw user location marker
            canvas.drawCircle(x, y, radius, userLocationPaint)
            canvas.drawCircle(x, y, radius, userLocationBorderPaint)
            
            // Draw direction indicator (small arrow pointing up)
            val arrowSize = 8f / scaleFactor
            canvas.drawLine(x, y - radius - arrowSize, x, y - radius - arrowSize * 2, userLocationBorderPaint)
            canvas.drawLine(x, y - radius - arrowSize * 2, x - arrowSize/2, y - radius - arrowSize * 1.5f, userLocationBorderPaint)
            canvas.drawLine(x, y - radius - arrowSize * 2, x + arrowSize/2, y - radius - arrowSize * 1.5f, userLocationBorderPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    translateX += dx
                    translateY += dy
                    
                    // Constrain translation to keep map visible
                    constrainTranslation()
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
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
        
        translateX = max(width - scaledWidth, min(0f, translateX))
        translateY = max(height - scaledHeight, min(0f, translateY))
    }
    
    private fun getMapCoordinates(screenX: Float, screenY: Float): Pair<Float, Float> {
        val mapX = (screenX - translateX) / scaleFactor
        val mapY = (screenY - translateY) / scaleFactor
        return Pair(mapX, mapY)
    }
    
    private fun findTreeAt(x: Float, y: Float): TreeLocationEntity? {
        val touchRadius = 30f / scaleFactor
        return trees.find { tree ->
            val distance = kotlin.math.sqrt(
                (tree.xCoordinate - x) * (tree.xCoordinate - x) +
                (tree.yCoordinate - y) * (tree.yCoordinate - y)
            )
            distance <= touchRadius
        }
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.3f, min(scaleFactor, 8.0f)) // Increased max zoom for detailed inspection
            
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
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val (mapX, mapY) = getMapCoordinates(e.x, e.y)
            
            // Check if a tree was clicked
            val clickedTree = findTreeAt(mapX, mapY)
            if (clickedTree != null) {
                onTreeClickListener?.invoke(clickedTree)
                selectTree(clickedTree.id)
                return true
            }
            
            // Check if click is within map bounds
            if (mapX >= 0 && mapX <= mapWidth && mapY >= 0 && mapY <= mapHeight) {
                onMapClickListener?.invoke(mapX, mapY)
                selectTree(null)
                return true
            }
            
            return false
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Reset zoom and center the map
            scaleFactor = 1.0f
            translateX = (width - mapWidth) / 2f
            translateY = (height - mapHeight) / 2f
            invalidate()
            return true
        }
    }
    
    private fun gpsToMapCoordinates(latitude: Double, longitude: Double, bounds: FloatArray): Pair<Float, Float> {
        val x = ((longitude - bounds[1]) / (bounds[3] - bounds[1]) * mapWidth).toFloat()
        val y = mapHeight - ((latitude - bounds[0]) / (bounds[2] - bounds[0]) * mapHeight).toFloat() // Invert Y for map coordinates
        return Pair(x.coerceIn(0f, mapWidth), y.coerceIn(0f, mapHeight))
    }
}
