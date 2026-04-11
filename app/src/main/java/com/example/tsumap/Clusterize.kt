package com.example.tsumap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.example.tsumap.Data.pointOfInterest
import kotlin.math.*

sealed class ClusterItem {
    abstract val screenPosition: Offset
    abstract val imagePosition: dataMap

    data class Single(
        val poi: pointOfInterest,
        override val screenPosition: Offset,
        override val imagePosition: dataMap
    ) : ClusterItem()

    data class Cluster(
        val items: List<pointOfInterest>,
        override val screenPosition: Offset,
        override val imagePosition: dataMap,
        val size: Int = items.size,
        val category: String
    ) : ClusterItem()
}

class PoiClusterer(
    private val density: Density,
    private val clusterRadius: Dp,
    private val minScaleToUnfoldSmallClusters: Float = 3.0f,
    private val smallClusterThreshold: Int = 3,
    private val categoryClassifier: (pointOfInterest) -> String = { it.type }
) {
    private val clusterRadiusPx: Float
        get() = with(density) { clusterRadius.toPx() }

    fun cluster(
        pois: List<pointOfInterest>,
        viewport: MapViewportState
    ): List<ClusterItem> {
        if (pois.isEmpty()) return emptyList()

        if (viewport.scale >= minScaleToUnfoldSmallClusters) {
            return pois.map { poi ->
                ClusterItem.Single(poi, viewport.imageToScreen(poi.pos), poi.pos)
            }
        }

        val byCategory = pois.groupBy(categoryClassifier)
        val allClusters = mutableListOf<ClusterItem>()

        for ((category, categoryPois) in byCategory) {
            allClusters.addAll(clusterCategory(category, categoryPois, viewport))
        }

        return allClusters
    }

    private fun clusterCategory(
        category: String,
        pois: List<pointOfInterest>,
        viewport: MapViewportState
    ): List<ClusterItem> {
        val scale = viewport.scale
        val imageRadius = clusterRadiusPx / scale

        val grid = mutableMapOf<Pair<Int, Int>, MutableList<pointOfInterest>>()
        pois.forEach { poi ->
            val cellX = (poi.pos.x / imageRadius).toInt()
            val cellY = (poi.pos.y / imageRadius).toInt()
            grid.getOrPut(cellX to cellY) { mutableListOf() }.add(poi)
        }

        val visited = mutableSetOf<pointOfInterest>()
        val clusters = mutableListOf<MutableList<pointOfInterest>>()

        for ((_, cellPois) in grid) {
            for (poi in cellPois) {
                if (poi in visited) continue

                val cluster = mutableListOf<pointOfInterest>()
                val queue = ArrayDeque<pointOfInterest>()
                queue.add(poi)
                visited.add(poi)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    cluster.add(current)

                    val cellX = (current.pos.x / imageRadius).toInt()
                    val cellY = (current.pos.y / imageRadius).toInt()

                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val neighborCell = (cellX + dx) to (cellY + dy)
                            grid[neighborCell]?.forEach { candidate ->
                                if (candidate !in visited) {
                                    val dxScreen = (current.pos.x - candidate.pos.x) * scale
                                    val dyScreen = (current.pos.y - candidate.pos.y) * scale
                                    val distScreen = sqrt(dxScreen * dxScreen + dyScreen * dyScreen)
                                    if (distScreen <= clusterRadiusPx) {
                                        queue.add(candidate)
                                        visited.add(candidate)
                                    }
                                }
                            }
                        }
                    }
                }
                clusters.add(cluster)
            }
        }

        return clusters.flatMap { clusterPoints ->
            when {
                clusterPoints.size == 1 -> {
                    val poi = clusterPoints.first()
                    listOf(ClusterItem.Single(poi, viewport.imageToScreen(poi.pos), poi.pos))
                }
                clusterPoints.size <= smallClusterThreshold && viewport.scale >= minScaleToUnfoldSmallClusters -> {
                    clusterPoints.map { poi ->
                        ClusterItem.Single(poi, viewport.imageToScreen(poi.pos), poi.pos)
                    }
                }
                else -> {
                    val avgX = clusterPoints.map { it.pos.x }.average().toFloat()
                    val avgY = clusterPoints.map { it.pos.y }.average().toFloat()
                    val imagePos = dataMap(avgX.roundToInt(), avgY.roundToInt())

                    listOf(
                        ClusterItem.Cluster(
                            items = clusterPoints,
                            screenPosition = viewport.imageToScreen(imagePos),
                            imagePosition = imagePos,
                            size = clusterPoints.size,
                            category = category
                        )
                    )
                }
            }
        }
    }
}