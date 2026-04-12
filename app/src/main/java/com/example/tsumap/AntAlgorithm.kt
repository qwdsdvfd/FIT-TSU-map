package com.example.tsumap

import com.example.tsumap.Data.pointOfInterest
import kotlin.math.sqrt
import kotlin.random.Random

data class TravelRoute(
    val wayPoints: List<pointOfInterest>,
    val totalDistance: Float,
    val userStartPos: dataMap
)

class AntAlgorithm(
    private val points: List<pointOfInterest>,
    private val userStartPos: dataMap
) {
    private val n = points.size
    private val distance = Array(n) { FloatArray(n) }
    private val pheromone = Array(n) { FloatArray(n) { 1f } }

    init {
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i == j) {
                    distance[i][j] = 0f
                } else {
                    val dx = (points[i].pos.x - points[j].pos.x).toFloat()
                    val dy = (points[i].pos.y - points[j].pos.y).toFloat()
                    distance[i][j] = sqrt(dx * dx + dy * dy)
                }
            }
        }
    }

    fun solve(antCount: Int = 20, iterations: Int = 50): TravelRoute {
        var bestPath = listOf<Int>()
        var bestDist = Float.MAX_VALUE

        repeat(iterations) {
            val ants = List(antCount) { Ant() }
            ants.forEach { it.buildPath() }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromone[i][j] *= 0.9f
                }
            }

            for (ant in ants) {
                val add = 10f / ant.totalDistance
                for (k in 0 until ant.path.size - 1) {
                    val from = ant.path[k]
                    val to = ant.path[k + 1]
                    pheromone[from][to] += add
                    pheromone[to][from] += add
                }
                if (ant.totalDistance < bestDist) {
                    bestDist = ant.totalDistance
                    bestPath = ant.path.toList()
                }
            }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromone[i][j] = pheromone[i][j].coerceIn(0.1f, 100f)
                }
            }
        }

        val resultPoints = bestPath.map { points[it] }
        return TravelRoute(resultPoints, bestDist, userStartPos)
    }

    private inner class Ant {
        val path = mutableListOf<Int>()
        var totalDistance = 0f
        private val visited = mutableSetOf<Int>()

        init {
            path.add(0)
            visited.add(0)
        }

        fun buildPath() {
            while (visited.size < n) {
                val current = path.last()
                val next = selectNext(current)
                if (next == -1) break
                path.add(next)
                visited.add(next)
            }
            path.add(0)
            calculateDistance()
        }

        private fun selectNext(current: Int): Int {
            val candidates = mutableListOf<Pair<Int, Float>>()
            var total = 0f
            for (next in 0 until n) {
                if (next !in visited) {
                    val phe = pheromone[current][next]
                    val vis = 1f / (distance[current][next] + 0.0001f)
                    val prob = phe * vis * vis
                    candidates.add(next to prob)
                    total += prob
                }
            }
            if (candidates.isEmpty()) return -1
            var rand = Random.nextFloat() * total
            for ((idx, p) in candidates) {
                rand -= p
                if (rand <= 0f) return idx
            }
            return candidates.last().first
        }

        private fun calculateDistance() {
            totalDistance = 0f
            for (i in 0 until path.size - 1) {
                totalDistance += distance[path[i]][path[i + 1]]
            }
        }
    }
}