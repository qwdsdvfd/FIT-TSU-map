package com.example.tsumap.data

import android.content.Context

object DataLoader {
    fun loadPointsOfInterest(context: Context): List<pointOfInterest> {
        return runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader().use { parsePointOfInterest(it.readText()) }
        }.getOrElse { emptyList() }
    }

    fun loadAttractions(context: Context): List<Attraction> {
        return runCatching {
            context.assets.open("База_данных_магазинов - Достопримечательности .csv")
                .bufferedReader().use { parseAttractions(it.readText()) }
        }.getOrElse { emptyList() }
    }

    fun loadShopHours(context: Context): Map<Int, String> {
        return runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader().use { reader ->
                    val lines = reader.readLines()
                    if (lines.isEmpty()) return@use emptyMap()
                    val header = lines.first().split(",")
                    val idIdx = header.indexOf("Id_place")
                    val hoursIdx = header.indexOf("raw_tags/opening_hours")
                    lines.drop(1).mapNotNull { line ->
                        val cols = line.split(",")
                        if (cols.size <= maxOf(idIdx, hoursIdx)) return@mapNotNull null
                        val id = cols[idIdx].toIntOrNull() ?: return@mapNotNull null
                        val hours = cols.getOrNull(hoursIdx)?.trim() ?: ""
                        id to hours
                    }.toMap()
                }
        }.getOrElse { emptyMap() }
    }
}