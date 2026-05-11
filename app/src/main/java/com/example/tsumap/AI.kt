package com.example.tsumap.algorithm

import android.content.Context
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

class AI {

    private val inputSize = 25
    private val hiddenSize1 = 32
    private val hiddenSize2 = 16
    private val outputSize = 10

    private val w1 = Array(hiddenSize1) { FloatArray(inputSize) }
    private val w2 = Array(hiddenSize2) { FloatArray(hiddenSize1) }
    private val w3 = Array(outputSize) { FloatArray(hiddenSize2) }

    private val b1 = FloatArray(hiddenSize1)
    private val b2 = FloatArray(hiddenSize2)
    private val b3 = FloatArray(outputSize)

    private val allExamples = mutableListOf<Pair<FloatArray, Int>>()
    private var trainedEpochs = 0

    init {
        initializeWeights()
    }

    private fun initializeWeights() {
        val random = Random(42)
        val scale1 = sqrt(2.0f / inputSize)
        val scale2 = sqrt(2.0f / hiddenSize1)
        val scale3 = sqrt(2.0f / hiddenSize2)

        for (i in 0 until hiddenSize1) {
            for (j in 0 until inputSize) {
                w1[i][j] = random.nextFloat() * 2 * scale1 - scale1
            }
            b1[i] = 0f
        }

        for (i in 0 until hiddenSize2) {
            for (j in 0 until hiddenSize1) {
                w2[i][j] = random.nextFloat() * 2 * scale2 - scale2
            }
            b2[i] = 0f
        }

        for (i in 0 until outputSize) {
            for (j in 0 until hiddenSize2) {
                w3[i][j] = random.nextFloat() * 2 * scale3 - scale3
            }
            b3[i] = 0f
        }
    }

    fun isReady(): Boolean = trainedEpochs >= 500

    private fun createMassiveExampleBase() {
        allExamples.clear()

        for (digit in 0..9) {
            addLargePatterns(digit)
            addMediumPatterns(digit)
            addSmallPatterns(digit)
        }
    }

    private fun addLargePatterns(digit: Int) {
        val patterns = when (digit) {
            0 -> listOf(
                listOf(0,1,1,1,0, 1,0,0,0,1, 1,0,0,0,1, 1,0,0,0,1, 0,1,1,1,0),
                listOf(1,1,1,1,1, 1,0,0,0,1, 1,0,0,0,1, 1,0,0,0,1, 1,1,1,1,1),
                listOf(1,1,1,1,1, 1,0,1,0,1, 1,0,0,0,1, 1,0,1,0,1, 1,1,1,1,1),
            )
            1 -> listOf(
                listOf(0,0,1,0,0, 0,1,1,0,0, 0,0,1,0,0, 0,0,1,0,0, 1,1,1,1,1),
                listOf(0,1,0,0,0, 1,1,0,0,0, 0,1,0,0,0, 0,1,0,0,0, 1,1,1,1,0),
                listOf(0,0,1,0,0, 0,0,1,0,0, 0,0,1,0,0, 0,0,1,0,0, 0,0,1,0,0),
            )
            2 -> listOf(
                listOf(1,1,1,1,1, 0,0,0,0,1, 1,1,1,1,1, 1,0,0,0,0, 1,1,1,1,1),
                listOf(0,1,1,1,0, 1,0,0,0,1, 0,0,0,1,0, 0,0,1,0,0, 1,1,1,1,1),
                listOf(1,1,1,1,1, 0,0,0,0,1, 0,1,1,1,1, 1,0,0,0,0, 1,1,1,1,1),
            )
            3 -> listOf(
                listOf(1,1,1,1,1, 0,0,0,0,1, 1,1,1,1,1, 0,0,0,0,1, 1,1,1,1,1),
                listOf(1,1,1,1,0, 0,0,0,0,1, 0,1,1,1,0, 0,0,0,0,1, 1,1,1,1,0),
                listOf(0,1,1,1,0, 0,0,0,0,1, 0,1,1,1,0, 0,0,0,0,1, 0,1,1,1,0),
            )
            4 -> listOf(
                listOf(1,0,0,0,1, 1,0,0,0,1, 1,1,1,1,1, 0,0,0,0,1, 0,0,0,0,1),
                listOf(0,1,0,1,0, 0,1,0,1,0, 1,1,1,1,1, 0,0,0,1,0, 0,0,0,1,0),
                listOf(1,0,0,1,0, 1,0,0,1,0, 1,1,1,1,0, 0,0,0,1,0, 0,0,0,1,0),
            )
            5 -> listOf(
                listOf(1,1,1,1,1, 1,0,0,0,0, 1,1,1,1,1, 0,0,0,0,1, 1,1,1,1,1),
                listOf(1,1,1,1,1, 1,0,0,0,0, 1,1,1,1,0, 0,0,0,0,1, 1,1,1,1,0),
                listOf(0,1,1,1,0, 1,0,0,0,0, 1,1,1,1,0, 0,0,0,0,1, 0,1,1,1,0),
            )
            6 -> listOf(
                listOf(1,1,1,1,1, 1,0,0,0,0, 1,1,1,1,1, 1,0,0,0,1, 1,1,1,1,1),
                listOf(0,1,1,1,0, 1,0,0,0,0, 1,1,1,1,0, 1,0,0,0,1, 0,1,1,1,0),
                listOf(1,1,1,1,0, 1,0,0,0,0, 1,1,1,1,1, 1,0,0,0,1, 0,1,1,1,0),
            )
            7 -> listOf(
                listOf(1,1,1,1,1, 0,0,0,0,1, 0,0,0,1,0, 0,0,1,0,0, 0,1,0,0,0),
                listOf(1,1,1,1,1, 0,0,0,0,1, 0,0,0,1,0, 0,1,0,0,0, 1,0,0,0,0),
                listOf(1,1,1,1,1, 0,0,0,1,0, 0,0,1,0,0, 0,1,0,0,0, 1,0,0,0,0),
            )
            8 -> listOf(
                listOf(1,1,1,1,1, 1,0,0,0,1, 1,1,1,1,1, 1,0,0,0,1, 1,1,1,1,1),
                listOf(0,1,1,1,0, 1,0,0,0,1, 0,1,1,1,0, 1,0,0,0,1, 0,1,1,1,0),
                listOf(1,1,1,1,1, 1,0,0,0,1, 0,1,1,1,0, 1,0,0,0,1, 1,1,1,1,1),
            )
            9 -> listOf(
                listOf(1,1,1,1,1, 1,0,0,0,1, 1,1,1,1,1, 0,0,0,0,1, 1,1,1,1,1),
                listOf(0,1,1,1,0, 1,0,0,0,1, 0,1,1,1,1, 0,0,0,0,1, 0,1,1,1,0),
                listOf(1,1,1,1,1, 1,0,0,0,1, 0,1,1,1,1, 0,0,0,0,1, 1,1,1,1,0),
            )
            else -> listOf()
        }

        patterns.forEach { pattern ->
            addWithAugmentation(digit, pattern, 15, 10)
        }
    }

    private fun addMediumPatterns(digit: Int) {
        val patterns = when (digit) {
            0 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,1,0,1,0, 0,1,1,1,0, 0,0,0,0,0),
                listOf(0,0,0,0,0, 0,1,1,0,0, 0,1,0,1,0, 0,0,1,1,0, 0,0,0,0,0),
            )
            1 -> listOf(
                listOf(0,0,0,0,0, 0,0,1,0,0, 0,1,1,0,0, 0,0,1,0,0, 0,0,0,0,0),
                listOf(0,0,0,0,0, 0,1,0,0,0, 0,1,0,0,0, 0,1,0,0,0, 0,0,0,0,0),
            )
            2 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,0,0,1,0, 0,1,1,0,0, 0,0,0,0,0),
                listOf(0,0,0,0,0, 0,1,1,0,0, 0,0,0,1,0, 0,1,1,0,0, 0,0,0,0,0),
            )
            3 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,0,1,0,0, 0,0,0,1,0, 0,1,1,0,0),
            )
            4 -> listOf(
                listOf(0,0,0,0,0, 0,1,0,1,0, 0,1,1,1,0, 0,0,0,1,0, 0,0,0,0,0),
            )
            5 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,1,0,0,0, 0,0,1,1,0, 0,0,0,0,0),
            )
            6 -> listOf(
                listOf(0,0,0,0,0, 0,0,1,1,0, 0,1,0,0,0, 0,1,1,1,0, 0,0,0,0,0),
            )
            7 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,0,0,1,0, 0,0,1,0,0, 0,0,0,0,0),
            )
            8 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,1,1,1,0, 0,1,0,1,0, 0,0,0,0,0),
            )
            9 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,1,0, 0,1,0,1,0, 0,0,0,1,0, 0,0,0,0,0),
            )
            else -> listOf()
        }

        patterns.forEach { pattern ->
            addWithAugmentation(digit, pattern, 20, 15)
        }
    }

    private fun addSmallPatterns(digit: Int) {
        val patterns = when (digit) {
            0 -> listOf(
                listOf(0,0,0,0,0, 0,0,0,0,0, 0,0,1,0,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            1 -> listOf(
                listOf(0,0,0,0,0, 0,0,1,0,0, 0,0,1,0,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            2 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,0,0, 0,0,0,1,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            3 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,0,0, 0,0,1,0,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            4 -> listOf(
                listOf(0,0,0,0,0, 0,1,0,1,0, 0,0,0,1,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            7 -> listOf(
                listOf(0,0,0,0,0, 0,1,1,0,0, 0,0,1,0,0, 0,0,0,0,0, 0,0,0,0,0),
            )
            else -> listOf()
        }

        patterns.forEach { pattern ->
            addWithAugmentation(digit, pattern, 25, 20)
        }
    }

    private fun addWithAugmentation(digit: Int, pattern: List<Int>, noiseCount: Int, shiftCount: Int) {
        val base = pattern.map { it.toFloat() }.toFloatArray()

        allExamples.add(base to digit)

        repeat(noiseCount) {
            allExamples.add(addNoise(base, 0.1f) to digit)
            allExamples.add(addNoise(base, 0.15f) to digit)
        }

        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx != 0 || dy != 0) {
                    repeat(shiftCount / 2) {
                        allExamples.add(shiftPattern(base, dx, dy) to digit)
                        allExamples.add(addNoise(shiftPattern(base, dx, dy), 0.1f) to digit)
                    }
                }
            }
        }

        repeat(3) {
            allExamples.add(thickenPattern(base) to digit)
        }
    }

    private fun addNoise(pattern: FloatArray, level: Float): FloatArray {
        return FloatArray(25) { i ->
            if (Random.nextFloat() < level) {
                if (pattern[i] > 0.5f) 0f else 1f
            } else {
                pattern[i]
            }
        }
    }

    private fun shiftPattern(pattern: FloatArray, dx: Int, dy: Int): FloatArray {
        val shifted = FloatArray(25) { 0f }
        for (y in 0 until 5) {
            for (x in 0 until 5) {
                if (pattern[y * 5 + x] > 0.5f) {
                    val ny = y + dy
                    val nx = x + dx
                    if (ny in 0..4 && nx in 0..4) {
                        shifted[ny * 5 + nx] = 1f
                    }
                }
            }
        }
        return shifted
    }

    private fun thickenPattern(p: FloatArray): FloatArray {
        val t = p.clone()
        for (y in 0 until 5) {
            for (x in 0 until 5) {
                if (p[y * 5 + x] > 0.5f) {
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val ny = y + dy
                            val nx = x + dx
                            if (ny in 0..4 && nx in 0..4) {
                                t[ny * 5 + nx] = 1f
                            }
                        }
                    }
                }
            }
        }
        return t
    }

    private fun normalizeInput(matrix: List<List<Boolean>>): FloatArray {
        var minX = 5; var maxX = -1
        var minY = 5; var maxY = -1

        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (matrix[i][j]) {
                    minX = minOf(minX, j)
                    maxX = maxOf(maxX, j)
                    minY = minOf(minY, i)
                    maxY = maxOf(maxY, i)
                }
            }
        }

        if (minX > maxX) return FloatArray(25) { 0f }

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        val result = FloatArray(25) { 0f }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val targetCenter = 2f

        val scale = when {
            width <= 2 && height <= 2 -> 2.5f
            width <= 3 && height <= 3 -> 1.8f
            width <= 4 && height <= 4 -> 1.3f
            else -> 1.0f
        }

        for (y in 0 until 5) {
            for (x in 0 until 5) {
                if (matrix[y][x]) {
                    val relX = x - centerX
                    val relY = y - centerY
                    val newX = (relX * scale + targetCenter).toInt().coerceIn(0, 4)
                    val newY = (relY * scale + targetCenter).toInt().coerceIn(0, 4)
                    result[newY * 5 + newX] = 1f
                }
            }
        }

        val pixelCount = result.count { it > 0.5f }
        return if (pixelCount < 5) thickenPattern(result) else result
    }

    private fun trainEpochs(epochs: Int, progressCallback: ((String) -> Unit)? = null) {
        val initialLR = 0.25f

        for (epoch in 0 until epochs) {
            val shuffled = allExamples.shuffled()
            val lr = initialLR * (1f - epoch.toFloat() / epochs)

            for ((input, target) in shuffled) {
                val hidden1 = FloatArray(hiddenSize1)
                for (i in 0 until hiddenSize1) {
                    var sum = b1[i]
                    for (j in 0 until inputSize) sum += w1[i][j] * input[j]
                    hidden1[i] = sigmoid(sum)
                }

                val dropMask1 = BooleanArray(hiddenSize1) { Random.nextFloat() > 0.2f }
                for (i in 0 until hiddenSize1) if (!dropMask1[i]) hidden1[i] = 0f

                val hidden2 = FloatArray(hiddenSize2)
                for (i in 0 until hiddenSize2) {
                    var sum = b2[i]
                    for (j in 0 until hiddenSize1) sum += w2[i][j] * hidden1[j]
                    hidden2[i] = sigmoid(sum)
                }

                val output = FloatArray(outputSize)
                for (i in 0 until outputSize) {
                    var sum = b3[i]
                    for (j in 0 until hiddenSize2) sum += w3[i][j] * hidden2[j]
                    output[i] = sigmoid(sum)
                }

                val expected = FloatArray(outputSize)
                expected[target] = 1f

                val outputError = FloatArray(outputSize)
                for (i in 0 until outputSize) {
                    outputError[i] = (expected[i] - output[i]) * sigmoidDeriv(output[i])
                }

                val hidden2Error = FloatArray(hiddenSize2)
                for (i in 0 until hiddenSize2) {
                    var error = 0f
                    for (j in 0 until outputSize) error += w3[j][i] * outputError[j]
                    hidden2Error[i] = error * sigmoidDeriv(hidden2[i])
                }

                val hidden1Error = FloatArray(hiddenSize1)
                for (i in 0 until hiddenSize1) {
                    if (!dropMask1[i]) continue
                    var error = 0f
                    for (j in 0 until hiddenSize2) error += w2[j][i] * hidden2Error[j]
                    hidden1Error[i] = error * sigmoidDeriv(hidden1[i])
                }

                for (i in 0 until outputSize) {
                    for (j in 0 until hiddenSize2) w3[i][j] += lr * outputError[i] * hidden2[j]
                    b3[i] += lr * outputError[i]
                }

                for (i in 0 until hiddenSize2) {
                    for (j in 0 until hiddenSize1) {
                        if (dropMask1[j]) w2[i][j] += lr * hidden2Error[i] * hidden1[j]
                    }
                    b2[i] += lr * hidden2Error[i]
                }

                for (i in 0 until hiddenSize1) {
                    if (!dropMask1[i]) continue
                    for (j in 0 until inputSize) w1[i][j] += lr * hidden1Error[i] * input[j]
                    b1[i] += lr * hidden1Error[i]
                }
            }

            trainedEpochs++

            if (progressCallback != null && (epoch + 1) % 25 == 0) {
                progressCallback("Обучение: ${(epoch + 1) * 100 / epochs}%")
            }
        }
        progressCallback?.invoke("Обучение завершено!")
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x.coerceIn(-10f, 10f)))
    private fun sigmoidDeriv(x: Float): Float = x * (1f - x)

    fun predict(matrix: List<List<Boolean>>): Pair<Int, FloatArray> {
        val input = normalizeInput(matrix)

        val hidden1 = FloatArray(hiddenSize1)
        for (i in 0 until hiddenSize1) {
            var sum = b1[i]
            for (j in 0 until inputSize) sum += w1[i][j] * input[j]
            hidden1[i] = sigmoid(sum)
        }

        val hidden2 = FloatArray(hiddenSize2)
        for (i in 0 until hiddenSize2) {
            var sum = b2[i]
            for (j in 0 until hiddenSize1) sum += w2[i][j] * hidden1[j]
            hidden2[i] = sigmoid(sum)
        }

        val output = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            var sum = b3[i]
            for (j in 0 until hiddenSize2) sum += w3[i][j] * hidden2[j]
            output[i] = sigmoid(sum)
        }

        val temperature = 0.2f
        val max = output.maxOrNull() ?: 0f
        val exp = FloatArray(outputSize) { i -> exp(((output[i] - max) / temperature).toDouble()).toFloat() }
        val sum = exp.sum()
        val probabilities = FloatArray(outputSize) { i -> exp[i] / sum }

        val predicted = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        return predicted to probabilities
    }

    fun train(input: List<List<Boolean>>, targetDigit: Int) {
        val normalized = normalizeInput(input)
        allExamples.add(normalized to targetDigit)
        repeat(10) { allExamples.add(addNoise(normalized, 0.15f) to targetDigit) }
        trainEpochs(20, null)
    }

    fun quickTrain(context: Context) {
        if (!isReady()) {
            createMassiveExampleBase()
            trainEpochs(600, null)
            saveWeights(context)
        }
    }

    fun resetAndRetrain(context: Context, progressCallback: (String) -> Unit) {
        initializeWeights()
        createMassiveExampleBase()
        trainedEpochs = 0
        trainEpochs(600, progressCallback)
        saveWeights(context)
    }

    fun saveWeights(context: Context) {
        val prefs = context.getSharedPreferences("nn_digits_v2", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        for (i in 0 until hiddenSize1) editor.putString("w1_$i", w1[i].joinToString(","))
        editor.putString("b1", b1.joinToString(","))
        for (i in 0 until hiddenSize2) editor.putString("w2_$i", w2[i].joinToString(","))
        editor.putString("b2", b2.joinToString(","))
        for (i in 0 until outputSize) editor.putString("w3_$i", w3[i].joinToString(","))
        editor.putString("b3", b3.joinToString(","))
        editor.putInt("trained_epochs", trainedEpochs)
        editor.apply()
    }

    fun loadWeights(context: Context): Boolean {
        val prefs = context.getSharedPreferences("nn_digits_v2", Context.MODE_PRIVATE)
        try {
            for (i in 0 until hiddenSize1) {
                val str = prefs.getString("w1_$i", null) ?: return false
                w1[i] = str.split(",").map { it.toFloat() }.toFloatArray()
            }
            b1.indices.forEach { b1[it] = prefs.getString("b1", "")!!.split(",")[it].toFloat() }

            for (i in 0 until hiddenSize2) {
                val str = prefs.getString("w2_$i", null) ?: return false
                w2[i] = str.split(",").map { it.toFloat() }.toFloatArray()
            }
            b2.indices.forEach { b2[it] = prefs.getString("b2", "")!!.split(",")[it].toFloat() }

            for (i in 0 until outputSize) {
                val str = prefs.getString("w3_$i", null) ?: return false
                w3[i] = str.split(",").map { it.toFloat() }.toFloatArray()
            }
            b3.indices.forEach { b3[it] = prefs.getString("b3", "")!!.split(",")[it].toFloat() }

            trainedEpochs = prefs.getInt("trained_epochs", 0)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}