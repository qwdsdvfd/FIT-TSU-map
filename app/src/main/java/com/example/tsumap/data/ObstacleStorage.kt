package com.example.tsumap.data

import android.content.Context
import com.example.tsumap.algorithm.dataMap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ObstacleStorage {
    private const val FILE = "obstacles.cache"

    suspend fun save(context: Context, obstacles: List<dataMap>) = withContext(Dispatchers.IO) {
        val str = obstacles.joinToString(";") { "${it.x},${it.y}" }
        File(context.cacheDir, FILE).writeText(str)
    }

    suspend fun load(context: Context): List<dataMap> = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, FILE)
        if (!file.exists()) return@withContext emptyList()
        val content = file.readText()
        if (content.isBlank()) return@withContext emptyList()
        content.split(";").mapNotNull { part ->
            val coords = part.split(",")
            if (coords.size == 2) {
                val x = coords[0].toIntOrNull()
                val y = coords[1].toIntOrNull()
                if (x != null && y != null) dataMap(x, y) else null
            } else null
        }
    }
}