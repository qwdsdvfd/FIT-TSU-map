package com.example.tsumap

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.data.*
import com.example.tsumap.algorithm.PUBLICMATRIX
import com.example.tsumap.algorithm.dataMap
import com.example.tsumap.navigation.NavigationManager
import com.example.tsumap.ui.components.AttractionSelectorDialog
import com.example.tsumap.ui.components.MapFromAssets
import com.example.tsumap.ui.components.MatrixDrawingDialog
import com.example.tsumap.ui.theme.TSUMAPTheme
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TSUMAPTheme { TSUMAPApp() } }
    }
}

@Composable
fun TSUMAPApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navManager = remember { NavigationManager(context, scope) }

    var startPoint by remember { mutableStateOf<dataMap?>(null) }
    var endPoint by remember { mutableStateOf<dataMap?>(null) }
    var pathPoints by remember { mutableStateOf<List<dataMap>>(emptyList()) }
    var obstacles by remember { mutableStateOf<List<dataMap>>(emptyList()) }

    LaunchedEffect(Unit) {
        navManager.start.collect { startPoint = it }
    }
    LaunchedEffect(Unit) {
        navManager.end.collect { endPoint = it }
    }
    LaunchedEffect(Unit) {
        navManager.path.collect { pathPoints = it }
    }
    LaunchedEffect(Unit) {
        navManager.obstacles.collect { obstacles = it }
    }

    var isSelectingStart by remember { mutableStateOf(false) }
    var isObstacleMode by remember { mutableStateOf(false) }
    var showAttractionSelector by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<pointOfInterest?>(null) }
    var showMatrixDialog by remember { mutableStateOf(false) }
    var matrix5x5 by remember { mutableStateOf(List(5) { List(5) { false } }) }

    val pointsOfInterest = remember {
        runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader().use { parsePointOfInterest(it.readText()) }
        }.getOrElse { emptyList() }
    }

    val attractions = remember {
        runCatching {
            context.assets.open("База_данных_магазинов - Достопримечательности .csv")
                .bufferedReader().use { parseAttractions(it.readText()) }
        }.getOrElse { emptyList() }
    }

    val attractionsAsPoI = remember { attractions.map { it.toPointOfInterest() } }
    val shopHoursMap = remember {
        runCatching {
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

    var selectedAttractionIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isRouting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val matrix = initBitMatrix(context)
        if (matrix == null) {
            Toast.makeText(context, "Ошибка загрузки карты проходимости", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }
        PUBLICMATRIX.set(matrix)
        val loaded = Matrix5x5Storage.load(context)
        if (loaded != null) matrix5x5 = loaded
        if (startPoint == null) {
            navManager.setStart(dataMap(1420, 2310))
        }
        navManager.onMatrixReady()
    }

    // Функция притягивания к ближайшему POI (магазин, супермаркет, достопримечательность)
    fun snapToNearestPoi(
        point: dataMap,
        pois: List<pointOfInterest>,
        radius: Int = 100
    ): dataMap {
        var nearest: pointOfInterest? = null
        var minDist = radius.toFloat()
        for (poi in pois) {
            val dx = poi.pos.x - point.x
            val dy = poi.pos.y - point.y
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist < minDist) {
                minDist = dist
                nearest = poi
            }
        }
        return nearest?.pos ?: point
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(
            pathPoints = pathPoints,
            startPoint = startPoint,
            endPoint = endPoint,
            pointsOfInterest = pointsOfInterest + attractionsAsPoI,
            obstacles = obstacles,
            selectedPoiIds = selectedAttractionIds,
            onPointOfInterestTap = { poi ->
                when {
                    poi.type == "sight" -> {
                        val new = selectedAttractionIds.toMutableSet()
                        if (poi.id in new) new.remove(poi.id) else new.add(poi.id)
                        selectedAttractionIds = new
                        Toast.makeText(context, if (poi.id in new) "Добавлено" else "Убрано", Toast.LENGTH_SHORT).show()
                    }
                    isSelectingStart -> {
                        navManager.setStart(poi.pos)
                        isSelectingStart = false
                        Toast.makeText(context, "Старт установлен", Toast.LENGTH_SHORT).show()
                    }
                    startPoint != null && endPoint == null -> {
                        navManager.setEnd(poi.pos)
                    }
                    else -> {
                        selectedPoi = poi
                    }
                }
            },
            onTap = { point ->
                // Притягивание к POI при выборе старта или финиша
                val allPois = pointsOfInterest + attractionsAsPoI
                val snappedPoint = if (isSelectingStart || (startPoint != null && endPoint == null)) {
                    snapToNearestPoi(point, allPois, 100)
                } else {
                    point
                }

                when {
                    isObstacleMode -> navManager.addObstacle(snappedPoint)
                    isSelectingStart -> {
                        navManager.setStart(snappedPoint)
                        isSelectingStart = false
                    }
                    startPoint != null && endPoint == null -> {
                        navManager.setEnd(snappedPoint)
                    }
                }
            },
            onDoubleTap = { navManager.reset() },
            onLongTap = { if (!isObstacleMode) navManager.removeObstacleAt(it) }
        )

        Row(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            FloatingActionButton(
                onClick = { isSelectingStart = true },
                containerColor = Color(0xFF3E55A2),
                modifier = Modifier.width(80.dp).height(48.dp)
            ) { Text("От/До", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { isObstacleMode = !isObstacleMode },
                containerColor = if (isObstacleMode) Color(0xFFFF5722) else Color(0xFF9E9E9E),
                modifier = Modifier.size(48.dp)
            ) { Text("-", fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { showAttractionSelector = true },
                containerColor = Color(0xFF2E7D32),
                modifier = Modifier.width(100.dp).height(48.dp)
            ) { Text("Маршрут", fontSize = 12.sp) }
        }
    }

    if (selectedPoi != null) {
        val poi = selectedPoi!!
        BottomSheetDialog(
            onDismiss = { selectedPoi = null },
            title = poi.name,
            text = "Часы работы: ${shopHoursMap[poi.id] ?: "Не указано"}",
            onFeedbackClick = { showMatrixDialog = true }
        )
    }

    if (showMatrixDialog) {
        MatrixDrawingDialog(
            matrix = matrix5x5,
            onToggleCell = { r, c ->
                matrix5x5 = matrix5x5.mapIndexed { i, row ->
                    if (i == r) row.mapIndexed { j, cell -> if (j == c) !cell else cell } else row
                }
            },
            onSave = {
                scope.launch { Matrix5x5Storage.save(context, matrix5x5) }
                showMatrixDialog = false
            },
            onDismiss = { showMatrixDialog = false }
        )
    }

    if (showAttractionSelector) {
        AttractionSelectorDialog(
            attractions = attractions,
            selectedIds = selectedAttractionIds,
            onSelectionChanged = { selectedAttractionIds = it },
            onRun = {
                scope.launch {
                    isRouting = true
                    val selected = attractionsAsPoI.filter { poi -> poi.id in selectedAttractionIds }
                    navManager.buildRouteThrough(selected) { route ->
                        isRouting = false
                        if (route != null) {
                            Toast.makeText(context, "Маршрут построен, длина ${route.totalDistance.toInt()} px", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Не удалось соединить точки", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAttractionSelector = false
                }
            },
            onDismiss = { showAttractionSelector = false },
            isRunning = isRouting
        )
    }
}

@Composable
fun BottomSheetDialog(onDismiss: () -> Unit, title: String, text: String, onFeedbackClick: () -> Unit) {
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFeedbackClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Сделать отзыв")
                    }
                }
            }
        }
    }
}