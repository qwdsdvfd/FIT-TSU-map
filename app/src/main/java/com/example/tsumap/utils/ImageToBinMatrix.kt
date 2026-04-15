package com.example.tsumap.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.tsumap.matrixToBit
import java.io.File
import java.util.BitSet

class matrixToBitCache(private val context: Context) {
    fun save(matrix: matrixToBit) {
        try {
            val file = File(context.filesDir, "BitMatrixSave_${matrix.height}_${matrix.width}.bin")
            file.writeBytes(matrix.data.toByteArray())
        } catch (e: Exception) {
            Log.e("BitMatrixCache", "Ошибка сохранения матрицы", e)
        }
    }

    fun load(): matrixToBit? {
        return try {
            val file = context.filesDir.listFiles()?.firstOrNull { it.name.startsWith("BitMatrixSave") }
                ?: return null
            val parts = file.name.removeSuffix(".bin").split("_")
            val height = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val width = parts.getOrNull(2)?.toIntOrNull() ?: return null
            matrixToBit(height, width, BitSet.valueOf(file.readBytes()))
        } catch (e: Exception) {
            Log.e("BitMatrixCache", "Ошибка загрузки матрицы", e)
            null
        }
    }
}

fun imageToBitMatrix(context: Context): matrixToBit? {
    return try {
        val bitmap = context.assets.open("skeleton.png").use { BitmapFactory.decodeStream(it) }
            ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val bitSet = BitSet(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = bitmap.getPixel(x, y)
                val bright = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
                if (bright < 60) {
                    bitSet.set(y * width + x)
                }
            }
        }

        val matrix = matrixToBit(height, width, bitSet)

        val filtered = matrixToBit(height, width, BitSet(width * height))
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix.get(x, y)) {
                    var hasNeighbor = false
                    if (x > 0 && matrix.get(x - 1, y)) hasNeighbor = true
                    if (x < width - 1 && matrix.get(x + 1, y)) hasNeighbor = true
                    if (y > 0 && matrix.get(x, y - 1)) hasNeighbor = true
                    if (y < height - 1 && matrix.get(x, y + 1)) hasNeighbor = true
                    if (hasNeighbor) {
                        filtered.set(x, y, true)
                    }
                }
            }
        }

        matrixToBitCache(context).save(filtered)
        filtered
    } catch (e: Exception) {
        Log.e("ImageToBitMatrix", "Не удалась конвертация изображения", e)
        null
    }
}

fun initBitMatrix(context: Context): matrixToBit? {
    return try {
        matrixToBitCache(context).load() ?: imageToBitMatrix(context)
    } catch (e: Exception) {
        Log.e("MATRIX", "Ошибка инициализации матрицы", e)
        imageToBitMatrix(context)
    }
}