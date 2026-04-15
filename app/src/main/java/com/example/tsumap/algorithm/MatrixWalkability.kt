package com.example.tsumap.algorithm

import com.example.tsumap.matrixToBit
import java.util.*

data class dataMap(val x: Int, val y: Int)

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

private fun matrixToBit.copyOf(): matrixToBit {
    val clonedBits = data.clone() as? BitSet
    return matrixToBit(height, width, clonedBits ?: BitSet())
}