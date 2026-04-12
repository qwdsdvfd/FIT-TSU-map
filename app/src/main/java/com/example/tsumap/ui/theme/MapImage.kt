package com.example.tsumap.ui.theme

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.tsumap.*
import com.example.tsumap.Data.pointOfInterest
import kotlin.math.roundToInt

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "food" -> Color(0xFFD2691E)
        "shop" -> Color(0xFF4169E1)
        "vending" -> Color(0xFFAA1AE8)
        else -> Color.Gray
    }
}

private fun getPoiColor(type: String): Color {
    return when {
        type == "sight" -> Color(0xFFFFD700)
        type in setOf("cafe", "restaurant", "fast_food", "ice_cream") -> Color(0xFFD2691E)
        type.startsWith("shop_") -> Color(0xFF4169E1)
        type.startsWith("vending_machine") -> Color(0xFFAA1AE8)
        else -> Color.Gray
    }
}

@Composable
fun MapFromAssets(
    pathPoints: List<dataMap>,
    startPoint: dataMap?,
    endPoint: dataMap?,
    pointsOfInterest: List<pointOfInterest> = emptyList(),
    obstacles: List<dataMap> = emptyList(),
    selectedPoiIds: Set<Int> = emptySet(),
    onPointOfInterestTap: (pointOfInterest) -> Unit = {},
    onTap: (dataMap) -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onLongTap: ((dataMap) -> Unit)? = null
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val viewportState = rememberMapViewportState()

    LaunchedEffect(Unit) {
        val bmp = context.assets.open("map_walk.png").use { BitmapFactory.decodeStream(it) }
        imageBitmap = bmp?.asImageBitmap()
    }

    val bitmap = imageBitmap ?: return

    LaunchedEffect(bitmap, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            viewportState.updateDimensions(
                bitmap.width,
                bitmap.height,
                canvasSize.width,
                canvasSize.height
            )
        }
    }

    val density = LocalDensity.current
    val clusterer = remember(density) {
        PoiClusterer(
            density = density,
            clusterRadius = 50.dp,
            minScaleToUnfoldSmallClusters = 2.8f,
            smallClusterThreshold = 2,
            categoryClassifier = { poi ->
                when {
                    poi.type in setOf("cafe", "restaurant", "fast_food", "ice_cream") -> "food"
                    poi.type.startsWith("shop_") -> "shop"
                    poi.type.startsWith("vending_machine") -> "vending"
                    else -> "other"
                }
            }
        )
    }
    val clusterItems = remember(viewportState.scale, viewportState.X, viewportState.Y, pointsOfInterest) {
        clusterer.cluster(pointsOfInterest, viewportState)
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewportState.panBy(pan.x, pan.y)
                    viewportState.zoomBy(
                        focus = Offset(size.width / 2f, size.height / 2f),
                        factor = zoom
                    )
                }
            }
            .pointerInput(clusterItems, onTap, onDoubleTap, onLongTap) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val hitItem = clusterItems.find { item ->
                            val dx = item.screenPosition.x - tapOffset.x
                            val dy = item.screenPosition.y - tapOffset.y
                            val radius = when (item) {
                                is ClusterItem.Single -> 28f
                                is ClusterItem.Cluster -> 38f
                            }
                            dx * dx + dy * dy <= radius * radius
                        }

                        when (hitItem) {
                            is ClusterItem.Single -> {
                                onPointOfInterestTap(hitItem.poi)
                            }
                            is ClusterItem.Cluster -> {
                                val focus = hitItem.screenPosition
                                viewportState.zoomBy(focus, 1.5f)
                            }
                            null -> {
                                val tapPixel = viewportState.screenToImage(tapOffset.x, tapOffset.y)
                                tapPixel?.let { onTap(it) }
                            }
                        }
                    },
                    onDoubleTap = { onDoubleTap?.invoke() },
                    onLongPress = { longPressOffset ->
                        val point = viewportState.screenToImage(longPressOffset.x, longPressOffset.y)
                        if (point != null) {
                            onLongTap?.invoke(point)
                        }
                    }
                )
            }
    ) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(
                viewportState.X.roundToInt(),
                viewportState.Y.roundToInt()
            ),
            dstSize = IntSize(
                (bitmap.width * viewportState.scale).roundToInt(),
                (bitmap.height * viewportState.scale).roundToInt()
            )
        )

        drawRoute(pathPoints, viewportState)
        drawMarker(startPoint, viewportState, Color(0xFF1B5E20), Color(0xFF66BB6A))
        drawMarker(endPoint, viewportState, Color(0xFF7F0000), Color(0xFFEF5350))
        drawClusterItems(clusterItems, viewportState, selectedPoiIds)
        drawObstacles(obstacles, viewportState)
    }
}

private fun DrawScope.drawRoute(points: List<dataMap>, viewport: MapViewportState) {
    if (points.size < 2) return
    val path = Path()
    val first = viewport.imageToScreen(points[0])
    path.moveTo(first.x, first.y)
    val step = maxOf(1, points.size / 3000)
    for (i in 1 until points.size step step) {
        val pt = viewport.imageToScreen(points[i])
        path.lineTo(pt.x, pt.y)
    }
    val last = viewport.imageToScreen(points.last())
    path.lineTo(last.x, last.y)
    drawPath(path, color = Color.Red, style = Stroke(width = 5f))
}

private fun DrawScope.drawMarker(
    point: dataMap?,
    viewport: MapViewportState,
    outerColor: Color,
    innerColor: Color
) {
    if (point == null) return
    val center = viewport.imageToScreen(point)
    val r = 14f
    drawCircle(color = outerColor, radius = r + 3f, center = center)
    drawCircle(color = innerColor, radius = r, center = center)
    drawCircle(color = Color.White, radius = r * 0.45f, center = center)
}

private fun DrawScope.drawClusterItems(
    items: List<ClusterItem>,
    viewport: MapViewportState,
    selectedPoiIds: Set<Int>
) {
    items.forEach { item ->
        when (item) {
            is ClusterItem.Single -> {
                drawSinglePoi(item.poi, item.screenPosition, selectedPoiIds)
            }
            is ClusterItem.Cluster -> {
                drawCluster(item, viewport)
            }
        }
    }
}

private fun DrawScope.drawSinglePoi(
    poi: pointOfInterest,
    screenPos: Offset,
    selectedPoiIds: Set<Int>
) {
    val markerRadius = 28f
    val isSelected = poi.id in selectedPoiIds
    val baseColor = getPoiColor(poi.type)

    drawCircle(baseColor, radius = markerRadius, center = screenPos)

    if (isSelected) {
        drawCircle(Color.Green, radius = markerRadius + 4f, center = screenPos, style = Stroke(width = 4f))
    }

    if (poi.type == "sight") {
        val starRadius = markerRadius * 0.6f
        val angles = (0..4).map { 90f - it * 72f }
        val outerPoints = angles.map { angle ->
            Offset(
                screenPos.x + starRadius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                screenPos.y + starRadius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            )
        }
        val innerRadius = starRadius * 0.4f
        val innerPoints = angles.map { angle ->
            Offset(
                screenPos.x + innerRadius * kotlin.math.cos(Math.toRadians((angle + 36).toDouble())).toFloat(),
                screenPos.y + innerRadius * kotlin.math.sin(Math.toRadians((angle + 36).toDouble())).toFloat()
            )
        }
        val starPath = Path().apply {
            for (i in 0..4) {
                if (i == 0) moveTo(outerPoints[i].x, outerPoints[i].y)
                else lineTo(outerPoints[i].x, outerPoints[i].y)
                lineTo(innerPoints[i].x, innerPoints[i].y)
            }
            close()
        }
        drawPath(starPath, color = Color.White)
    } else {
        drawCircle(Color.White, radius = 6f, center = screenPos)
    }

    drawContext.canvas.nativeCanvas.apply {
        val textSize = 45f
        val paintStroke = Paint().apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        drawText(poi.name, screenPos.x + 32f, screenPos.y + 12f, paintStroke)
        val paintFill = Paint().apply {
            color = android.graphics.Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        drawText(poi.name, screenPos.x + 32f, screenPos.y + 12f, paintFill)
    }
}

private fun DrawScope.drawCluster(cluster: ClusterItem.Cluster, viewport: MapViewportState) {
    val center = cluster.screenPosition
    val radius = 38f
    val clusterColor = getCategoryColor(cluster.category)

    drawCircle(color = clusterColor, radius = radius, center = center)
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3f))

    drawContext.canvas.nativeCanvas.apply {
        val text = cluster.size.toString()
        val textSize = 36f
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        drawText(text, center.x, center.y - (textBounds.top + textBounds.bottom) / 2f, paint)
    }

    drawContext.canvas.nativeCanvas.apply {
        val text = "точек"
        val textSize = 20f
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        drawText(text, center.x, center.y + radius + 20f, paint)
    }
}

private fun DrawScope.drawObstacles(
    obstacles: List<dataMap>,
    viewport: MapViewportState
) {
    val radius = 8f
    for (obs in obstacles) {
        val center = viewport.imageToScreen(obs)
        drawCircle(color = Color(0xCCFF5722), radius = radius, center = center)
        drawCircle(color = Color(0xFFBF360C), radius = radius, center = center, style = Stroke(width = 2f))
    }
}