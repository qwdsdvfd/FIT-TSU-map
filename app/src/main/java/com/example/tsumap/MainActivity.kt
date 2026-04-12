package com.example.tsumap

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.Data.Matrix5x5Storage
import com.example.tsumap.Data.Attraction
import com.example.tsumap.Data.parseAttractions
import com.example.tsumap.Data.toPointOfInterest
import com.example.tsumap.Data.parsePointOfInterest
import com.example.tsumap.Data.pointOfInterest
import com.example.tsumap.ui.theme.MapFromAssets
import com.example.tsumap.ui.theme.MatrixDrawingDialog
import com.example.tsumap.ui.theme.TSUMAPTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TSUMAPTheme { TSUMAPApp() } }
    }
}

@Composable
fun TSUMAPApp() {
    var obstacles by remember { mutableStateOf<List<dataMap>>(emptyList()) }
    var isObstacleMode by remember { mutableStateOf(false) }
    var startPoint by remember { mutableStateOf<dataMap?>(null) }
    var endPoint by remember { mutableStateOf<dataMap?>(null) }
    var pathPoints by remember { mutableStateOf<List<dataMap>>(emptyList()) }
    var isSelectingStart by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<pointOfInterest?>(null) }
    var showBottomDialog by remember { mutableStateOf(false) }
    var showMatrixDialog by remember { mutableStateOf(false) }
    var matrix5x5 by remember { mutableStateOf(List(5) { List(5) { false } }) }
    var showAttractionSelector by remember { mutableStateOf(false) }
    var selectedAttractions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isAntAlgorithmRunning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val attractionsAsPoI = remember {
        attractions.map { it.toPointOfInterest() }
    }

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

    suspend fun saveObstacles(context: Context, points: List<dataMap>) = withContext(Dispatchers.IO) {
        File(context.cacheDir, "obstacles.cache").writeText(
            points.joinToString(separator = ";") { "${it.x},${it.y}" }
        )
    }

    suspend fun loadObstacles(context: Context): List<dataMap> = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "obstacles.cache")
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

    fun computePath() {
        val mat = PUBLICMATRIX.value ?: return
        val start = startPoint ?: return
        val end = endPoint ?: return
        pathPoints = aStar(mat, start, end) ?: emptyList()
    }

    fun runAntAlgorithm() {
        if (selectedAttractions.isEmpty()) {
            Toast.makeText(context, "Выберите хотя бы одну достопримечательность", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isAntAlgorithmRunning = true
            val selectedPoints = attractionsAsPoI.filter { it.id in selectedAttractions }
            val userPoint = pointOfInterest(
                id = -1,
                name = "Ваша позиция",
                type = "user",
                pos = startPoint ?: dataMap(0, 0)
            )
            val allPoints = listOf(userPoint) + selectedPoints
            val algorithm = AntAlgorithm(allPoints, startPoint ?: dataMap(0, 0))
            val result = algorithm.solve(antCount = 30, iterations = 80)

            pathPoints = result.wayPoints.map { it.pos }
            Toast.makeText(
                context,
                "Маршрут построен! Длина: ${String.format("%.0f", result.totalDistance)} px",
                Toast.LENGTH_LONG
            ).show()

            isAntAlgorithmRunning = false
            showAttractionSelector = false
        }
    }

    fun addObstacle(point: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val snapped = findNearWay(mat, point) ?: return
        if (obstacles.any { it.x == snapped.x && it.y == snapped.y }) return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = snapped.x + dx; val y = snapped.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) PUBLICMATRIX.addObstruction(x, y)
                }
            }
        }
        obstacles += snapped
        scope.launch { saveObstacles(context, obstacles) }
        if (endPoint != null) computePath()
        Toast.makeText(context, "Препятствие добавлено", Toast.LENGTH_SHORT).show()
    }

    fun removeObstacleAt(point: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val toRemove = obstacles.find { obs ->
            val dx = obs.x - point.x; val dy = obs.y - point.y; dx*dx + dy*dy <= 100
        } ?: return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = toRemove.x + dx; val y = toRemove.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) {
                        val original = PUBLICMATRIX.value ?: continue
                        if (original.get(x, y)) PUBLICMATRIX.value?.set(x, y, true)
                    }
                }
            }
        }
        obstacles -= toRemove
        scope.launch { saveObstacles(context, obstacles) }
        if (endPoint != null) computePath()
        Toast.makeText(context, "Препятствие удалено", Toast.LENGTH_SHORT).show()
    }

    fun resetAll() {
        startPoint = null; endPoint = null; pathPoints = emptyList()
        isSelectingStart = false; isObstacleMode = false
    }

    LaunchedEffect(Unit) {
        val matrix = initBitMatrix(context)
        PUBLICMATRIX.set(matrix)
        val loaded = loadObstacles(context)
        obstacles = loaded
        loaded.forEach { PUBLICMATRIX.addObstruction(it.x, it.y) }

        val loadedMatrix = Matrix5x5Storage.load(context)
        if (loadedMatrix != null) matrix5x5 = loadedMatrix

        if (startPoint == null) {
            startPoint = dataMap(1420, 2310)
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(
            pathPoints = pathPoints, startPoint = startPoint, endPoint = endPoint,
            pointsOfInterest = pointsOfInterest.plus(attractionsAsPoI), obstacles = obstacles,
            selectedPoiIds = selectedAttractions,
            onPointOfInterestTap = { poi ->
                if (poi.type == "sight") {
                    val newSelection = selectedAttractions.toMutableSet()
                    if (poi.id in newSelection) {
                        newSelection.remove(poi.id)
                        Toast.makeText(context, "Убрано из маршрута: ${poi.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        newSelection.add(poi.id)
                        Toast.makeText(context, "Добавлено в маршрут: ${poi.name}", Toast.LENGTH_SHORT).show()
                    }
                    selectedAttractions = newSelection
                } else {
                    if (isSelectingStart) {
                        startPoint = poi.pos; endPoint = null; pathPoints = emptyList(); isSelectingStart = false
                    } else if (startPoint != null && endPoint == null) {
                        endPoint = poi.pos; pathPoints = emptyList(); computePath()
                    } else {
                        selectedPoi = poi; showBottomDialog = true
                    }
                }
            },
            onTap = { point ->
                when {
                    isObstacleMode -> addObstacle(point)
                    isSelectingStart -> {
                        startPoint = point; endPoint = null; pathPoints = emptyList(); isSelectingStart = false
                    }
                    startPoint != null && endPoint == null -> {
                        endPoint = point; pathPoints = emptyList(); computePath()
                    }
                    else -> {}
                }
            },
            onDoubleTap = { resetAll() },
            onLongTap = { point -> if (!isObstacleMode) removeObstacleAt(point) }
        )

        Row(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            FloatingActionButton(
                onClick = { if (startPoint == null && !isSelectingStart) isSelectingStart = true else { resetAll(); isSelectingStart = true } },
                containerColor = Color(0xFF3E55A2), contentColor = Color.White,
                modifier = Modifier.width(80.dp).height(48.dp)
            ) { Text("От/До", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { isObstacleMode = !isObstacleMode },
                containerColor = if (isObstacleMode) Color(0xFFFF5722) else Color(0xFF9E9E9E),
                contentColor = Color.White, modifier = Modifier.size(48.dp)
            ) { Text("-", fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { showAttractionSelector = true },
                containerColor = Color(0xFF2E7D32), contentColor = Color.White,
                modifier = Modifier.width(100.dp).height(48.dp)
            ) { Text("Маршрут", fontSize = 12.sp) }
        }
    }

    if (showBottomDialog && selectedPoi != null) {
        val poi = selectedPoi!!
        val hoursRaw = shopHoursMap[poi.id] ?: ""
        val hoursDisplay = if (hoursRaw.isBlank()) "Не указано" else hoursRaw

        androidx.compose.ui.window.Popup(
            onDismissRequest = { showBottomDialog = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .clickable(onClick = { showBottomDialog = false }, indication = null,
                        interactionSource = remember { MutableInteractionSource() }),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp).navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(poi.name, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Часы работы: $hoursDisplay", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showMatrixDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Сделать отзыв") }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
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
            selectedIds = selectedAttractions,
            onSelectionChanged = { selectedAttractions = it },
            onRun = { runAntAlgorithm() },
            onDismiss = { showAttractionSelector = false },
            isRunning = isAntAlgorithmRunning
        )
    }
}

@Composable
fun AttractionSelectorDialog(
    attractions: List<Attraction>,
    selectedIds: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    isRunning: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите достопримечательности", fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Найдено: ${attractions.size} достопримечательностей",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(attractions) { attraction ->
                        val isSelected = attraction.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = selectedIds.toMutableSet()
                                    if (isSelected) newSelection.remove(attraction.id)
                                    else newSelection.add(attraction.id)
                                    onSelectionChanged(newSelection)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    val newSelection = selectedIds.toMutableSet()
                                    if (it) newSelection.add(attraction.id)
                                    else newSelection.remove(attraction.id)
                                    onSelectionChanged(newSelection)
                                }
                            )
                            Column(modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)) {
                                Text(
                                    text = attraction.name,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Рейтинг: ${attraction.rating}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRun,
                enabled = !isRunning && selectedIds.isNotEmpty(),
                modifier = Modifier.width(120.dp)
            ) {
                if (isRunning) {
                    Text("Загрузка...")
                } else {
                    Text("Построить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRunning) {
                Text("Отмена")
            }
        }
    )
}