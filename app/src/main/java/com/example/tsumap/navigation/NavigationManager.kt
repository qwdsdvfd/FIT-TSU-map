package com.example.tsumap.navigation

import android.content.Context
import com.example.tsumap.data.pointOfInterest
import com.example.tsumap.algorithm.AntOptimizer
import com.example.tsumap.algorithm.OptimizedRoute
import com.example.tsumap.algorithm.PUBLICMATRIX
import com.example.tsumap.data.ObstacleStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.example.tsumap.algorithm.aStar
import com.example.tsumap.algorithm.dataMap
import com.example.tsumap.algorithm.findNearWay

class NavigationManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Исходные точки (где кликнул пользователь) – для отображения маркеров
    private val _rawStart = MutableStateFlow<dataMap?>(null)
    val start: StateFlow<dataMap?> = _rawStart

    private val _rawEnd = MutableStateFlow<dataMap?>(null)
    val end: StateFlow<dataMap?> = _rawEnd

    private val _path = MutableStateFlow<List<dataMap>>(emptyList())
    val path: StateFlow<List<dataMap>> = _path

    private val _obstacles = MutableStateFlow<List<dataMap>>(emptyList())
    val obstacles: StateFlow<List<dataMap>> = _obstacles

    init {
        scope.launch {
            _obstacles.value = ObstacleStorage.load(context)
            _obstacles.value.forEach { PUBLICMATRIX.addObstruction(it.x, it.y) }
        }
    }

    // Установка старта – сохраняем точку как есть, без изменения
    fun setStart(point: dataMap) {
        _rawStart.value = point
        _rawEnd.value = null
        _path.value = emptyList()
    }

    // Установка финиша – сохраняем точку как есть, без изменения
    fun setEnd(point: dataMap) {
        if (_rawStart.value != null) {
            _rawEnd.value = point
            computePath()
        }
    }

    fun addObstacle(center: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val snapped = findNearWay(mat, center) ?: return
        if (_obstacles.value.any { it.x == snapped.x && it.y == snapped.y }) return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = snapped.x + dx
                    val y = snapped.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) {
                        PUBLICMATRIX.addObstruction(x, y)
                    }
                }
            }
        }
        _obstacles.value += snapped
        scope.launch { ObstacleStorage.save(context, _obstacles.value) }
        if (_rawEnd.value != null) computePath()
    }

    fun removeObstacleAt(point: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val toRemove = _obstacles.value.find { obs ->
            val dx = obs.x - point.x
            val dy = obs.y - point.y
            dx*dx + dy*dy <= 100
        } ?: return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = toRemove.x + dx
                    val y = toRemove.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) {
                        PUBLICMATRIX.value?.set(x, y, true)
                    }
                }
            }
        }
        _obstacles.value -= toRemove
        scope.launch { ObstacleStorage.save(context, _obstacles.value) }
        if (_rawEnd.value != null) computePath()
    }

    fun reset() {
        _rawStart.value = null
        _rawEnd.value = null
        _path.value = emptyList()
    }

    fun buildRouteThrough(pois: List<pointOfInterest>, onComplete: (OptimizedRoute?) -> Unit) {
        val rawStart = _rawStart.value ?: return onComplete(null)
        // Для построения маршрута берём проходимую версию старта
        val walkableStart = getWalkable(rawStart) ?: return onComplete(null)
        val optimizer = AntOptimizer(pois, walkableStart)
        val route = optimizer.optimize()
        val fullPath = buildFullRoute(route.points)
        if (fullPath.isNotEmpty()) {
            _path.value = fullPath
            onComplete(route)
        } else {
            onComplete(null)
        }
    }

    private fun getWalkable(point: dataMap): dataMap? {
        val mat = PUBLICMATRIX.value ?: return null
        return if (mat.get(point.x, point.y)) point else findNearWay(mat, point)
    }

    private fun computePath() {
        val mat = PUBLICMATRIX.value ?: return
        val rawS = _rawStart.value ?: return
        val rawE = _rawEnd.value ?: return

        // Для пути используем ближайшие проходимые клетки, но исходные точки не меняем
        val walkableStart = getWalkable(rawS) ?: return
        val walkableEnd = getWalkable(rawE) ?: return

        _path.value = aStar(mat, walkableStart, walkableEnd) ?: emptyList()
    }

    private fun buildFullRoute(orderedPoints: List<pointOfInterest>): List<dataMap> {
        val matrix = PUBLICMATRIX.value ?: return emptyList()
        if (orderedPoints.size < 2) return emptyList()
        val full = mutableListOf<dataMap>()
        for (i in 0 until orderedPoints.size - 1) {
            val from = orderedPoints[i].pos
            val to = orderedPoints[i+1].pos
            val segment = aStar(matrix, from, to) ?: return emptyList()
            if (full.isEmpty()) full.addAll(segment)
            else full.addAll(segment.drop(1))
        }
        return full
    }

    fun onMatrixReady() {
        if (_rawStart.value != null && _rawEnd.value != null) {
            computePath()
        }
    }
}