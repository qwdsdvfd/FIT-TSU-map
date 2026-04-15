package com.example.tsumap.data

import com.example.tsumap.algorithm.dataMap

data class Attraction(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val address: String,
    val rating: Float
)

fun parseAttractions(csvContent: String): List<Attraction> {
    return csvContent.lineSequence()
        .drop(1) // Пропустить заголовок
        .mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 7) return@mapNotNull null

            runCatching {
                Attraction(
                    id = parts[0].trim().toInt(),
                    name = parts[1].trim().removeSurrounding("\""),
                    latitude = parts[2].trim().toDouble(),
                    longitude = parts[3].trim().toDouble(),
                    type = parts[4].trim(),
                    address = parts[5].trim().removeSurrounding("\""),
                    rating = parts[6].trim().toFloatOrNull() ?: 0f
                )
            }.getOrNull()
        }
        .toList()
}

fun Attraction.toPointOfInterest(): pointOfInterest {
    val x = ((this.longitude - MapBounds.minLength) / (MapBounds.maxWidth - MapBounds.minLength) * MapBounds.W).toInt()
    val y = ((MapBounds.maxLength - this.latitude) / (MapBounds.maxLength - MapBounds.minWidth) * MapBounds.H).toInt()
    
    return pointOfInterest(
        id = this.id,
        name = this.name,
        type = this.type,
        pos = dataMap(x.coerceIn(0, 2840), y.coerceIn(0, 4619))
    )
}
