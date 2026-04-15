package com.example.tsumap.algorithm

import com.example.tsumap.matrixToBit
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max

fun aStar(matrix: matrixToBit, start: dataMap, end: dataMap): List<dataMap>? {
    val w = matrix.width
    val h = matrix.height
    if (start.x !in 0 until w || start.y !in 0 until h) return null
    if (end.x !in 0 until w || end.y !in 0 until h) return null

    val start1 = if (matrix.get(start.x, start.y)) start else findNearWay(matrix, start) ?: return null
    val safeEnd = if (matrix.get(end.x, end.y)) end else findNearWay(matrix, end) ?: return null
    if (start1 == safeEnd) return listOf(start1)

    val gScore = hashMapOf<dataMap, Float>()
    val fScore = hashMapOf<dataMap, Float>()
    val cameFrom = hashMapOf<dataMap, dataMap>()
    val openSet = PriorityQueue<dataMap>(compareBy { fScore[it] ?: Float.MAX_VALUE })
    val inOpen = hashSetOf<dataMap>()

    gScore[start1] = 0f
    fScore[start1] = heuristic(start1, safeEnd)
    openSet.add(start1)
    inOpen.add(start1)

    while (openSet.isNotEmpty()) {
        val current = openSet.poll() ?: continue

        if (fScore[current] != null && fScore[current] != (gScore[current] ?: 0f) + heuristic(current, safeEnd)) {
            continue
        }
        inOpen.remove(current)

        if (current == safeEnd) return reconstructPath(cameFrom, safeEnd)

        for ((neighbor, cost) in neighbours(current, w, h, matrix)) {
            val tentative = (gScore[current] ?: Float.MAX_VALUE) + cost
            if (tentative < (gScore[neighbor] ?: Float.MAX_VALUE)) {
                cameFrom[neighbor] = current
                gScore[neighbor] = tentative
                val newF = tentative + heuristic(neighbor, safeEnd)
                fScore[neighbor] = newF

                if (neighbor in inOpen) {
                    openSet.remove(neighbor)
                } else {
                    inOpen.add(neighbor)
                }
                openSet.add(neighbor)
            }
        }
    }
    return null
}

private fun heuristic(a: dataMap, b: dataMap): Float = max(abs(a.x - b.x), abs(a.y - b.y)).toFloat()

private fun neighbours(point: dataMap, w: Int, h: Int, matrix: matrixToBit): List<Pair<dataMap, Float>> {
    val moves = listOf(
        dataMap(1,0) to 1f, dataMap(-1,0) to 1f, dataMap(0,1) to 1f, dataMap(0,-1) to 1f,
        dataMap(1,1) to 1.414f, dataMap(1,-1) to 1.414f, dataMap(-1,1) to 1.414f, dataMap(-1,-1) to 1.414f
    )
    return moves.mapNotNull { (shift, cost) ->
        val nx = point.x + shift.x
        val ny = point.y + shift.y
        if (nx in 0 until w && ny in 0 until h && matrix.get(nx, ny)) dataMap(nx, ny) to cost else null
    }
}

private fun reconstructPath(cameFrom: Map<dataMap, dataMap>, end: dataMap): List<dataMap> {
    val path = ArrayDeque<dataMap>()
    var node: dataMap? = end
    while (node != null) {
        path.addFirst(node)
        node = cameFrom[node]
    }
    return path.toList()
}