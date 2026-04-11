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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.tsumap.Data.pointOfInterest
import com.example.tsumap.MapViewportState
import com.example.tsumap.dataMap
import com.example.tsumap.rememberMapViewportState
import kotlin.math.roundToInt

private fun getPoiColor(type: String): Color {
    return when {
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
    onPointOfInterestTap: (pointOfInterest) -> Unit = {},
    onTap: (dataMap) -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onLongTap: ((dataMap) -> Unit)? = null
)
{
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
            .pointerInput(onTap, onDoubleTap, onLongTap) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val tapPixel = viewportState.screenToImage(tapOffset.x, tapOffset.y)
                        if (tapPixel != null) {
                            val hitPoint = pointsOfInterest.find {
                                val dx = it.pos.x - tapPixel.x
                                val dy = it.pos.y - tapPixel.y
                                dx * dx + dy * dy < 400
                            }
                            if (hitPoint != null) {
                                onPointOfInterestTap(hitPoint)
                            } else {
                                onTap(tapPixel)
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
    )
    {
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
        drawPointsOfInterest(pointsOfInterest, viewportState)
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
)
{
    if (point == null) return
    val center = viewport.imageToScreen(point)
    val r = 14f
    drawCircle(color = outerColor, radius = r + 3f, center = center)
    drawCircle(color = innerColor, radius = r, center = center)
    drawCircle(color = Color.White, radius = r * 0.45f, center = center)
}

private fun DrawScope.drawPointsOfInterest(
    points: List<pointOfInterest>,
    viewport: MapViewportState
)
{
    points.forEach { poi ->
        val screenPos = viewport.imageToScreen(poi.pos)
        val markerRadius = 28f
        drawCircle(getPoiColor(poi.type), radius = markerRadius, center = screenPos)
        drawCircle(Color.White, radius = 6f, center = screenPos)

        drawContext.canvas.nativeCanvas.apply{
            val textSize = 45f
            val paintStroke = Paint().apply{
                color = android.graphics.Color.WHITE
                this.textSize = textSize
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            drawText(
                poi.name,
                screenPos.x + 32f,
                screenPos.y + 12f,
                paintStroke
            )
            val paintFill = Paint().apply{
                color = android.graphics.Color.BLACK
                this.textSize = textSize
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            drawText(
                poi.name,
                screenPos.x + 32f,
                screenPos.y + 12f,
                paintFill
            )
        }
    }
}

private fun DrawScope.drawObstacles(
    obstacles: List<dataMap>,
    viewport: MapViewportState
)
{
    val radius = 8f
    for (obs in obstacles) {
        val center = viewport.imageToScreen(obs)
        drawCircle(color = Color(0xCCFF5722), radius = radius, center = center)
        drawCircle(color = Color(0xFFBF360C), radius = radius, center = center, style = Stroke(width = 2f))
    }
}