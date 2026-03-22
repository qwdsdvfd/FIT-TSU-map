package com.example.tsumap

import android.content.Context
import java.util.BitSet
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import android.util.Log

class BitMatrix(private val height: Int, private val width: Int, private val bitSet: BitSet) {
    val data = bitSet
    val datawidth = width
    val dataheight = height

    fun get(x: Int, y: Int): Boolean = data[y * width + x]

    fun set(x: Int, y: Int, value: Boolean){
        if (value) data.set(y * width + x)
        else data.clear(y * width + x)
    }
}

class BitMatrixCache(private val context: Context){
    fun save(matrix: BitMatrix){
        val file = File(context.filesDir, "BitMatrixSave_${matrix.dataheight}_${matrix.datawidth}.bin")
        file.writeBytes(matrix.data.toByteArray())
    }

    fun load(): BitMatrix?{
        val file = context.filesDir.listFiles()?.firstOrNull { it.name.startsWith("BitMatrixSave") }
        return if (file != null){
            val trueName = file.name.removeSuffix(".bin")
            val height = trueName.split("_")[1].toInt()
            val width = trueName.split("_")[2].toInt()
            BitMatrix(height, width, BitSet.valueOf(file.readBytes()))
        }
        else null
    }
}

fun imageToBitMatrix(context: Context): BitMatrix? {
    val bitmap = context.assets.open("skeleton.png").use { input -> BitmapFactory.decodeStream(input)}
    if (bitmap == null) return null
    val width = bitmap.width
    val height = bitmap.height
    val totalPixels = width * height
    val bitSet = BitSet(totalPixels)

    for (y in 0 until height) {
        for (x in 0 until width) {
            if ((Color.red(bitmap.getPixel(x, y))) > 125) {
                bitSet.set(y * width + x)
            }
        }
    }

    val cache = BitMatrixCache(context)
    val matrix = BitMatrix(height, width, bitSet)

    cache.save(matrix)

    return matrix
}

// МОЖНО СКАЗАТЬ ОПТИМИЗАЕЙШОН
fun initBitMatrix(context: Context): BitMatrix?{
    // ПЫТАЕМСЯ ЗАГРУЗИТЬ КЕШУ
    val cache = BitMatrixCache(context).load()

    return try{ cache }
    catch (e: Exception){
        Log.e("BitMatrix", "Ошибка чтения кэша: ${e.message}")


        // НЕ СМОГЛИ - ЗАГРУЗКА БИНКИ
        return try{
            imageToBitMatrix(context)
        }
        catch (e: Exception){
            Log.e("BitMatrix", "Ошибка создания матрицы из изображения: ${e.message}")
            null
        }
    }
}