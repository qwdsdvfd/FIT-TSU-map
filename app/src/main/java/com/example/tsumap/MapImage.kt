package com.example.tsumap

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

private data class ImageLayout(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun computeLayout(imgW: Float, imgH: Float, canvasW: Float, canvasH: Float): ImageLayout {
    val scale = min(canvasW / imgW, canvasH / imgH)
    return ImageLayout(scale, (canvasW - imgW * scale) / 2f, (canvasH - imgH * scale) / 2f)
}

private fun screenToImage(sx: Float, sy: Float, l: ImageLayout): dataMap =
    dataMap(((sx - l.offsetX) / l.scale).roundToInt(), ((sy - l.offsetY) / l.scale).roundToInt())

private fun imageToScreen(p: dataMap, l: ImageLayout): Offset =
    Offset(p.x * l.scale + l.offsetX, p.y * l.scale + l.offsetY)

@Composable
fun MapFromAssets(
    pathPoints: List<dataMap>,
    startPoint: dataMap?,
    endPoint: dataMap?,
    onTap: (dataMap) -> Unit,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        val bmp = context.assets.open("map_walk.png").use { BitmapFactory.decodeStream(it) }
        imageBitmap = bmp?.asImageBitmap()
    }

    val bitmap = imageBitmap ?: return

    val layout = remember(bitmap, canvasSize) {
        computeLayout(
            bitmap.width.toFloat(), bitmap.height.toFloat(),
            canvasSize.width.toFloat(), canvasSize.height.toFloat()
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(onTap, onDoubleTap) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val point = screenToImage(tapOffset.x, tapOffset.y, layout)
                        if (point.x in 0 until bitmap.width && point.y in 0 until bitmap.height) {
                            onTap(point)
                        }
                    },
                    onDoubleTap = {
                        onDoubleTap?.invoke()
                    }
                )
            }
    ) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(layout.offsetX.roundToInt(), layout.offsetY.roundToInt()),
            dstSize = IntSize(
                (bitmap.width * layout.scale).roundToInt(),
                (bitmap.height * layout.scale).roundToInt()
            )
        )

        if (pathPoints.size > 1) {
            drawRoute(pathPoints, layout)
        }

        if (startPoint != null) {
            drawMarker(startPoint, layout, Color(0xFF1B5E20), Color(0xFF66BB6A))
        }

        if (endPoint != null) {
            drawMarker(endPoint, layout, Color(0xFF7F0000), Color(0xFFEF5350))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoute(points: List<dataMap>, layout: ImageLayout) {
    val path = Path()
    val first = imageToScreen(points[0], layout)
    path.moveTo(first.x, first.y)
    val step = maxOf(1, points.size / 3000)
    for (i in 1 until points.size step step) {
        val pt = imageToScreen(points[i], layout)
        path.lineTo(pt.x, pt.y)
    }
    val last = imageToScreen(points.last(), layout)
    path.lineTo(last.x, last.y)
    drawPath(path, color = Color.Red, style = Stroke(width = 5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarker(point: dataMap, layout: ImageLayout, outerColor: Color, innerColor: Color) {
    val center = imageToScreen(point, layout)
    val r = maxOf(14f, 14f * layout.scale)
    drawCircle(color = outerColor, radius = r + 3f, center = center)
    drawCircle(color = innerColor, radius = r, center = center)
    drawCircle(color = Color.White, radius = r * 0.45f, center = center)
}