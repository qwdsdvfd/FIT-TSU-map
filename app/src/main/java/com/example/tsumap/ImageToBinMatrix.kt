package com.example.tsumap

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import java.io.File
import java.util.BitSet

class matrixToBit(val height: Int, val width: Int, val data: BitSet) {
    fun get(x: Int, y: Int): Boolean = data[y * width + x]
    fun set(x: Int, y: Int, value: Boolean) {
        if (value) data.set(y * width + x) else data.clear(y * width + x)
    }
}

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
                if (bright > 128) bitSet.set(y * width + x)
            }
        }
        val matrix = matrixToBit(height, width, bitSet)
        matrixToBitCache(context).save(matrix)
        matrix
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