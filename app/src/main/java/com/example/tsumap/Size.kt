package com.example.tsumap

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max

class MapViewportState(
    initialScale: Float = 1f,
    private val minScale: Float = 0.5f,
    private val maxScale: Float = 4f
)
{
    var scale by mutableFloatStateOf(initialScale.coerceIn(minScale, maxScale))
        private set

    var X by mutableFloatStateOf(0f)
        private set
    var Y by mutableFloatStateOf(0f)
        private set

    var imageWidth by mutableIntStateOf(0)
    var imageHeight by mutableIntStateOf(0)

    var canvasWidth by mutableIntStateOf(0)
    var canvasHeight by mutableIntStateOf(0)

    fun updateDimensions(imageWidth: Int, imageHeight: Int, canvasWidth: Int, canvasHeight: Int) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight
        clampOffsets()
    }

    fun panBy(deltaX: Float, deltaY: Float) {
        X += deltaX
        Y += deltaY
        clampOffsets()
    }

    fun zoomBy(focus: Offset, factor: Float) {
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        if (abs(newScale - scale) < 0.001f) return

        val focusOnImageX = (focus.x - X) / scale
        val focusOnImageY = (focus.y - Y) / scale

        scale = newScale

        X = focus.x - focusOnImageX * scale
        Y = focus.y - focusOnImageY * scale

        clampOffsets()
    }

    private fun clampOffsets() {
        if (imageWidth == 0 || imageHeight == 0) return
        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale

        val maxOverflow = max(canvasWidth, canvasHeight) * 0.2f
        val minX = canvasWidth - scaledW - maxOverflow
        val maxX = maxOverflow
        val minY = canvasHeight - scaledH - maxOverflow
        val maxY = maxOverflow

        X = X.coerceIn(minX, maxX)
        Y = Y.coerceIn(minY, maxY)
    }

    fun screenToImage(screenX: Float, screenY: Float): dataMap? {
        val imgX = ((screenX - X) / scale).toInt()
        val imgY = ((screenY - Y) / scale).toInt()
        if (imgX in 0 until imageWidth && imgY in 0 until imageHeight) {
            return dataMap(imgX, imgY)
        }
        return null
    }

    fun imageToScreen(imagePoint: dataMap): Offset {
        return Offset(
            imagePoint.x * scale + X,
            imagePoint.y * scale + Y
        )
    }
}

@Composable
fun rememberMapViewportState(
    initialScale: Float = 1f,
    minScale: Float = 0.5f,
    maxScale: Float = 4f
): MapViewportState {
    return remember { MapViewportState(initialScale, minScale, maxScale) }
}