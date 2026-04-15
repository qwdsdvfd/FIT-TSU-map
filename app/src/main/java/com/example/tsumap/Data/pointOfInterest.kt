package com.example.tsumap.data

import com.example.tsumap.algorithm.dataMap
import com.example.tsumap.algorithm.PUBLICMATRIX
import com.example.tsumap.algorithm.findNearWay

data class pointOfInterest(val id: Int, val name: String, val type: String, val pos: dataMap)

object MapBounds {
    const val W = 2841f
    const val H = 4620f
    const val minLength = 84.93852f
    const val maxWidth = 84.95591f
    const val maxLength = 56.47387f
    const val minWidth = 56.45837f
}

fun gpsToPixel(lat: Double, lon: Double): dataMap {
    val x = ((lon - MapBounds.minLength) / (MapBounds.maxWidth - MapBounds.minLength) * MapBounds.W).toInt()
    val y = ((MapBounds.maxLength - lat) / (MapBounds.maxLength - MapBounds.minWidth) * MapBounds.H).toInt()
    return dataMap(x.coerceIn(0, 2840), y.coerceIn(0, 4619))
}

fun parsePointOfInterest(csvContent: String): List<pointOfInterest> = csvContent.lineSequence()
    .drop(1)
    .mapNotNull { line ->
        val p = line.split(",")
        if (p.size < 5) return@mapNotNull null
        runCatching {
            val rawPos = gpsToPixel(p[1].toDouble(), p[2].toDouble())
            val walkablePos = PUBLICMATRIX.value?.let { matrix ->
                if (matrix.get(rawPos.x, rawPos.y)) rawPos else findNearWay(matrix, rawPos)
            } ?: rawPos
            pointOfInterest(
                id = p[0].toInt(),
                name = p[3].trim().removeSurrounding("\""),
                type = p[4].trim(),
                pos = walkablePos ?: rawPos
            )
        }.getOrNull()
    }.toList()