package com.example.tsumap

import java.util.PriorityQueue
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max

object PUBLICMATRIX {
    private var firstMatrix: matrixToBit? = null
    var value: matrixToBit? = null
        private set

    fun set(matrix: matrixToBit?) {
        firstMatrix = matrix
        value = matrix?.copyOf()
    }

    fun addObstruction(x: Int, y: Int) {
        value?.set(x, y, false)
    }

    fun resetObstruction() {
        value = firstMatrix?.copyOf()
    }
}

data class dataMap(val x: Int, val y: Int)

private fun heuristic(a: dataMap, b: dataMap): Float {
    val dx = abs(a.x - b.x)
    val dy = abs(a.y - b.y)
    return max(dx, dy).toFloat()
}

fun findNearWay(matrix: matrixToBit, point: dataMap): dataMap? {
    if (matrix.get(point.x, point.y)) return point

    val w = matrix.width
    val h = matrix.height
    val visited = Array(w) { BooleanArray(h) }
    val queue = LinkedList<dataMap>()
    queue.add(point)
    visited[point.x][point.y] = true

    while (queue.isNotEmpty()) {
        val cur = queue.poll()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = cur.x + dx
                val ny = cur.y + dy
                if (nx in 0 until w && ny in 0 until h && !visited[nx][ny]) {
                    visited[nx][ny] = true
                    if (matrix.get(nx, ny)) {
                        return dataMap(nx, ny)
                    }
                    queue.add(dataMap(nx, ny))
                }
            }
        }
    }
    return null
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

private fun neighbours(point: dataMap, w: Int, h: Int, matrix: matrixToBit): List<Pair<dataMap, Float>> {
    val moves = arrayOf(
        dataMap(1, 0) to 1.0f, dataMap(-1, 0) to 1.0f, dataMap(0, 1) to 1.0f, dataMap(0, -1) to 1.0f,
        dataMap(1, 1) to 1.414f, dataMap(1, -1) to 1.414f, dataMap(-1, 1) to 1.414f, dataMap(-1, -1) to 1.414f
    )
    val result = mutableListOf<Pair<dataMap, Float>>()
    for ((shift, cost) in moves) {
        val nx = point.x + shift.x
        val ny = point.y + shift.y
        if (nx in 0 until w && ny in 0 until h && matrix.get(nx, ny)) {
            result.add(dataMap(nx, ny) to cost)
        }
    }
    return result
}

fun aStar(matrix: matrixToBit, start: dataMap, end: dataMap): List<dataMap>? {
    val w = matrix.width
    val h = matrix.height

    if (start.x !in 0 until w || start.y !in 0 until h) return null
    if (end.x !in 0 until w || end.y !in 0 until h) return null

    val start1 = if (matrix.get(start.x, start.y)) start else findNearWay(matrix, start) ?: return null
    val safeEnd = if (matrix.get(end.x, end.y)) end else findNearWay(matrix, end) ?: return null
    if (start1 == safeEnd) return listOf(start1)

    val gScore = HashMap<dataMap, Float>(1024)
    val fScore = HashMap<dataMap, Float>(1024)
    val cameFrom = HashMap<dataMap, dataMap>(1024)
    val inOpen = HashSet<dataMap>(1024)
    val closed = HashSet<dataMap>(1024)
    val open = PriorityQueue<dataMap>(compareBy { fScore[it] ?: Float.MAX_VALUE })

    gScore[start1] = 0f
    fScore[start1] = heuristic(start1, safeEnd)
    open.add(start1)
    inOpen.add(start1)

    while (open.isNotEmpty()) {
        val cur = open.poll() ?: break
        inOpen.remove(cur)
        if (cur == safeEnd) {
            return reconstructPath(cameFrom, safeEnd)
        }
        closed.add(cur)
        for ((nb, cost) in neighbours(cur, w, h, matrix)) {
            if (nb in closed) continue
            val tentativeG = (gScore[cur] ?: Float.MAX_VALUE) + cost
            if (tentativeG < (gScore[nb] ?: Float.MAX_VALUE)) {
                cameFrom[nb] = cur
                gScore[nb] = tentativeG
                fScore[nb] = tentativeG + heuristic(nb, safeEnd)
                if (nb !in inOpen) {
                    open.add(nb)
                    inOpen.add(nb)
                }
            }
        }
    }
    return null
}

private fun matrixToBit.copyOf(): matrixToBit {
    val clonedBits = data.clone() as? java.util.BitSet
    return matrixToBit(height, width, clonedBits ?: java.util.BitSet())
}