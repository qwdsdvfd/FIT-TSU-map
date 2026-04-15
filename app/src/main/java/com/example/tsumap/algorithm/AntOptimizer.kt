package com.example.tsumap.algorithm

import com.example.tsumap.data.pointOfInterest
import kotlin.math.sqrt
import kotlin.random.Random

data class OptimizedRoute(
    val points: List<pointOfInterest>,
    val totalDistance: Float,
    val startPosition: dataMap
)

class AntOptimizer(
    private val targets: List<pointOfInterest>,
    private val userPos: dataMap
) {
    private val allPoints = listOf(userPoi()) + targets
    private val n = allPoints.size
    private val dist = Array(n) { FloatArray(n) }
    private val pheromone = Array(n) { FloatArray(n) { 1f } }

    private fun userPoi() = pointOfInterest(-1, "Моё местоположение", "user", userPos)

    init {
        val matrix = PUBLICMATRIX.value
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = if (matrix != null) {
                    val path = aStar(matrix, allPoints[i].pos, allPoints[j].pos)
                    if (path != null) pathDistance(path) else euclidean(allPoints[i], allPoints[j])
                } else {
                    euclidean(allPoints[i], allPoints[j])
                }
                dist[i][j] = d
                dist[j][i] = d
            }
        }
    }

    private fun euclidean(a: pointOfInterest, b: pointOfInterest): Float {
        val dx = (a.pos.x - b.pos.x).toFloat()
        val dy = (a.pos.y - b.pos.y).toFloat()
        return sqrt(dx * dx + dy * dy)
    }

    private fun pathDistance(path: List<dataMap>): Float {
        var sum = 0f
        for (i in 0 until path.size - 1) {
            val dx = (path[i+1].x - path[i].x).toFloat()
            val dy = (path[i+1].y - path[i].y).toFloat()
            sum += sqrt(dx*dx + dy*dy)
        }
        return sum
    }

    fun optimize(antCount: Int = 25, iterations: Int = 60): OptimizedRoute {
        var bestIndices = listOf<Int>()
        var bestDist = Float.MAX_VALUE

        repeat(iterations) {
            val ants = List(antCount) { Ant() }
            ants.forEach { it.build() }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromone[i][j] *= 0.9f
                }
            }

            for (ant in ants) {
                val deposit = 10f / ant.totalDist
                for (k in 0 until ant.path.size - 1) {
                    val a = ant.path[k]
                    val b = ant.path[k+1]
                    pheromone[a][b] += deposit
                    pheromone[b][a] += deposit
                }
                if (ant.totalDist < bestDist) {
                    bestDist = ant.totalDist
                    bestIndices = ant.path.toList()
                }
            }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromone[i][j] = pheromone[i][j].coerceIn(0.1f, 100f)
                }
            }
        }

        val resultPoints = bestIndices.map { allPoints[it] }
        return OptimizedRoute(resultPoints, bestDist, userPos)
    }

    private inner class Ant {
        val path = mutableListOf<Int>()
        var totalDist = 0f
        private val visited = mutableSetOf<Int>()

        init {
            path.add(0)
            visited.add(0)
        }

        fun build() {
            while (visited.size < n) {
                val current = path.last()
                val next = selectNext(current)
                if (next == -1) break
                path.add(next)
                visited.add(next)
            }
            path.add(0)
            var sum = 0f
            for (i in 0 until path.size - 1) {
                sum += dist[path[i]][path[i + 1]]
            }
            totalDist = sum
        }

        private fun selectNext(current: Int): Int {
            val candidates = mutableListOf<Pair<Int, Float>>()
            var totalProb = 0f
            for (next in 0 until n) {
                if (next !in visited) {
                    val prob = pheromone[current][next] / (dist[current][next] + 0.0001f)
                    candidates.add(next to prob)
                    totalProb += prob
                }
            }
            if (candidates.isEmpty()) return -1
            var r = Random.nextFloat() * totalProb
            for ((idx, p) in candidates) {
                r -= p
                if (r <= 0f) return idx
            }
            return candidates.last().first
        }
    }
}