package com.example.tsumap.ui.components

import androidx.compose.runtime.Composable
import com.example.tsumap.data.pointOfInterest
import com.example.tsumap.algorithm.dataMap
import com.example.tsumap.ui.theme.MapFromAssets as ThemeMapFromAssets

@Composable
fun MapFromAssets(
    pathPoints: List<dataMap>,
    startPoint: dataMap?,
    endPoint: dataMap?,
    pointsOfInterest: List<pointOfInterest> = emptyList(),
    obstacles: List<dataMap> = emptyList(),
    selectedPoiIds: Set<Int> = emptySet(),
    onPointOfInterestTap: (pointOfInterest) -> Unit = {},
    onTap: (dataMap) -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onLongTap: ((dataMap) -> Unit)? = null
) {
    ThemeMapFromAssets(
        pathPoints = pathPoints,
        startPoint = startPoint,
        endPoint = endPoint,
        pointsOfInterest = pointsOfInterest,
        obstacles = obstacles,
        selectedPoiIds = selectedPoiIds,
        onPointOfInterestTap = onPointOfInterestTap,
        onTap = onTap,
        onDoubleTap = onDoubleTap,
        onLongTap = onLongTap
    )
}