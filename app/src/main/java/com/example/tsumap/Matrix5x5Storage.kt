package com.example.tsumap

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Matrix5x5Storage {
    private const val FILE_NAME = "matrix_5x5.cache"
    private const val SIZE = 5

    suspend fun save(context: Context, matrix: List<List<Boolean>>) = withContext(Dispatchers.IO) {
        val flatData = matrix.flatten().joinToString(",") { if (it) "1" else "0" }
        File(context.filesDir, FILE_NAME).writeText(flatData)
    }

    suspend fun load(context: Context): List<List<Boolean>>? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@withContext null
        val content = file.readText().takeIf { it.isNotBlank() } ?: return@withContext null
        val bits = content.split(",").map { it.trim() == "1" }
        if (bits.size != SIZE * SIZE) return@withContext null
        List(SIZE) { row -> bits.subList(row * SIZE, (row + 1) * SIZE) }
    }
}