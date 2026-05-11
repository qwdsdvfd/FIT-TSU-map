package com.example.tsumap

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class TspResult(val order: List<Int>, val totalCost: Double)

class AntColony(
    private val distanceMatrix: Array<DoubleArray>,
    private val alpha: Double = 1.0,
    private val beta: Double = 2.0,
    private val evaporation: Double = 0.5,
    private val pheromoneFactor: Double = 100.0,
    private val antCount: Int = 20,
    private val iterations: Int = 100
) {
    private val cityCount = distanceMatrix.size
    private val pheromone = Array(cityCount) { DoubleArray(cityCount) { 1.0 } }
    private val random = Random.Default

    private fun heuristic(i: Int, j: Int): Double {
        val dist = distanceMatrix[i][j]
        return if (dist <= 0.0) 1e-10 else 1.0 / dist
    }

    fun solve(): TspResult {
        var bestOrder = listOf<Int>()
        var bestCost = Double.MAX_VALUE

        repeat(iterations) {
            val antPaths = mutableListOf<List<Int>>()
            val antCosts = mutableListOf<Double>()

            repeat(antCount) {
                val path = buildAntPath()
                val cost = pathCost(path)
                antPaths.add(path)
                antCosts.add(cost)

                if (cost < bestCost) {
                    bestCost = cost
                    bestOrder = path
                }
            }

            evaporatePheromone()
            depositPheromone(antPaths, antCosts)
        }

        return TspResult(bestOrder, bestCost)
    }

    private fun buildAntPath(): List<Int> {
        val unvisited = (0 until cityCount).toMutableSet()
        val start = random.nextInt(cityCount)
        unvisited.remove(start)
        val path = mutableListOf(start)

        while (unvisited.isNotEmpty()) {
            val current = path.last()
            val next = selectNextCity(current, unvisited)
            path.add(next)
            unvisited.remove(next)
        }
        return path
    }

    private fun selectNextCity(current: Int, candidates: Set<Int>): Int {
        val probabilities = mutableListOf<Pair<Int, Double>>()
        var total = 0.0

        for (next in candidates) {
            val pher = pheromone[current][next].pow(alpha)
            val heur = heuristic(current, next).pow(beta)
            val value = pher * heur
            probabilities.add(next to value)
            total += value
        }

        if (total == 0.0) return candidates.random(random)

        val rand = random.nextDouble(total)
        var acc = 0.0
        for ((city, prob) in probabilities) {
            acc += prob
            if (rand <= acc) return city
        }
        return probabilities.last().first
    }

    private fun pathCost(path: List<Int>): Double {
        var cost = 0.0
        for (i in 0 until path.size - 1) {
            cost += distanceMatrix[path[i]][path[i + 1]]
        }
        return cost
    }

    private fun evaporatePheromone() {
        for (i in 0 until cityCount) {
            for (j in 0 until cityCount) {
                pheromone[i][j] *= (1.0 - evaporation)
            }
        }
    }

    private fun depositPheromone(antPaths: List<List<Int>>, antCosts: List<Double>) {
        for (pathIndex in antPaths.indices) {
            val path = antPaths[pathIndex]
            val cost = antCosts[pathIndex]
            val contribution = pheromoneFactor / cost

            for (i in 0 until path.size - 1) {
                val from = path[i]
                val to = path[i + 1]
                pheromone[from][to] += contribution
                pheromone[to][from] += contribution
            }
        }
    }
}

fun buildDistanceMatrix(
    selectedPois: List<dataMap>,
    matrix: matrixToBit
): Array<DoubleArray>? {
    val n = selectedPois.size
    val distances = Array(n) { DoubleArray(n) { Double.POSITIVE_INFINITY } }

    for (i in 0 until n) {
        distances[i][i] = 0.0
        for (j in i + 1 until n) {
            val start = selectedPois[i]
            val end = selectedPois[j]
            val path = aStar(matrix, start, end)
            if (path != null) {
                var length = 0.0
                for (k in 1 until path.size) {
                    val p1 = path[k - 1]
                    val p2 = path[k]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    length += sqrt((dx * dx + dy * dy).toDouble())
                }
                distances[i][j] = length
                distances[j][i] = length
            } else {
                return null
            }
        }
    }
    return distances
}