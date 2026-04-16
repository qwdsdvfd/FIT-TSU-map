package com.example.tsumap.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RatingStorage {
    private const val SHOPS_CSV = "База_данных_магазинов - Магазины.csv"

    suspend fun saveRating(context: Context, shopId: Int, newRating: Int) = withContext(Dispatchers.IO) {
        try {
            val csvFile = File(context.filesDir, SHOPS_CSV)

            if (!csvFile.exists()) {
                copyFromAssets(context, csvFile)
            }

            val lines = csvFile.readLines().toMutableList()
            if (lines.isEmpty()) return@withContext

            val headers = lines[0].split(",").toMutableList()

            var ratingIndex = headers.indexOf("rating")
            var countIndex = headers.indexOf("rating_count")

            if (ratingIndex == -1) {
                headers.add("rating")
                ratingIndex = headers.size - 1
            }
            if (countIndex == -1) {
                headers.add("rating_count")
                countIndex = headers.size - 1
            }

            lines[0] = headers.joinToString(",")

            var shopLineIndex = -1
            var shopParts = mutableListOf<String>()
            val allRatings = mutableListOf<Int>()

            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.isNotEmpty() && parts[0].toIntOrNull() == shopId) {
                    shopLineIndex = i
                    shopParts = parts.toMutableList()

                    if (countIndex < parts.size) {
                        val existingCount = parts[countIndex].toIntOrNull() ?: 0
                        if (existingCount > 0 && ratingIndex < parts.size) {
                            val existingRating = parts[ratingIndex].toFloatOrNull() ?: 0f
                            repeat(existingCount) {
                                allRatings.add(existingRating.toInt())
                            }
                        }
                    }
                    break
                }
            }

            if (shopLineIndex == -1) {
                println("Shop $shopId not found in CSV")
                return@withContext
            }

            allRatings.add(newRating)

            val averageRating = allRatings.sum().toFloat() / allRatings.size

            while (shopParts.size <= maxOf(ratingIndex, countIndex)) {
                shopParts.add("")
            }

            shopParts[ratingIndex] = String.format("%.1f", averageRating)
            shopParts[countIndex] = allRatings.size.toString()

            lines[shopLineIndex] = shopParts.joinToString(",")

            csvFile.writeText(lines.joinToString("\n"))

            println("Rating saved: Shop $shopId, new rating: $newRating, average: $averageRating, count: ${allRatings.size}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyFromAssets(context: Context, file: File) {
        try {
            context.assets.open(SHOPS_CSV).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRating(context: Context, shopId: Int): ShopRating? = withContext(Dispatchers.IO) {
        try {
            val csvFile = File(context.filesDir, SHOPS_CSV)
            if (!csvFile.exists()) return@withContext null

            val lines = csvFile.readLines()
            if (lines.size < 2) return@withContext null

            val headers = lines[0].split(",")
            val ratingIndex = headers.indexOf("rating")
            val countIndex = headers.indexOf("rating_count")

            if (ratingIndex == -1) return@withContext null

            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.isNotEmpty() && parts[0].toIntOrNull() == shopId) {
                    val rating = if (ratingIndex < parts.size) {
                        parts[ratingIndex].toFloatOrNull() ?: 0f
                    } else 0f

                    val count = if (countIndex != -1 && countIndex < parts.size) {
                        parts[countIndex].toIntOrNull() ?: 0
                    } else 0

                    return@withContext if (count > 0) ShopRating(rating, count) else null
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllRatings(context: Context): Map<Int, ShopRating> = withContext(Dispatchers.IO) {
        try {
            val csvFile = File(context.filesDir, SHOPS_CSV)
            if (!csvFile.exists()) return@withContext emptyMap()

            val lines = csvFile.readLines()
            if (lines.size < 2) return@withContext emptyMap()

            val headers = lines[0].split(",")
            val ratingIndex = headers.indexOf("rating")
            val countIndex = headers.indexOf("rating_count")

            if (ratingIndex == -1) return@withContext emptyMap()

            val result = mutableMapOf<Int, ShopRating>()

            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.isEmpty()) continue

                val id = parts[0].toIntOrNull() ?: continue

                val rating = if (ratingIndex < parts.size) {
                    parts[ratingIndex].toFloatOrNull() ?: 0f
                } else 0f

                val count = if (countIndex != -1 && countIndex < parts.size) {
                    parts[countIndex].toIntOrNull() ?: 0
                } else 0

                if (count > 0 && rating > 0) {
                    result[id] = ShopRating(rating, count)
                }
            }

            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

data class ShopRating(
    val average: Float,
    val count: Int
)