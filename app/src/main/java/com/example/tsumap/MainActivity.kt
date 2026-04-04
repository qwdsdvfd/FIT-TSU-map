package com.example.tsumap

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.ui.theme.TSUMAPTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TSUMAPTheme {
                TSUMAPApp()
            }
        }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pointsOfInterest = remember {
        runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader()
                .use { parsePointOfInterest(it.readText()) }
        }.getOrElse { emptyList() }
    }

    val shopHoursMap = remember {
        runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader()
                .use { reader ->
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

    suspend fun saveObstacles(context: Context, points: List<dataMap>) {
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "obstacles.cache")
            val data = points.joinToString(separator = ";") { "${it.x},${it.y}" }
            file.writeText(data)
        }
    }

    suspend fun loadObstacles(context: Context): List<dataMap> {
        return withContext(Dispatchers.IO) {
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
    }

    fun computePath() {
        val mat = PUBLICMATRIX.value ?: return
        val start = startPoint ?: return
        val end = endPoint ?: return
        val result = aStar(mat, start, end)
        pathPoints = result ?: emptyList()
    }

    fun addObstacle(point: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val snapped = findNearWay(mat, point) ?: return
        if (obstacles.any { it.x == snapped.x && it.y == snapped.y }) return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = snapped.x + dx
                    val y = snapped.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) {
                        PUBLICMATRIX.addObstruction(x, y)
                    }
                }
            }
        }
        val newObstacles = obstacles + snapped
        obstacles = newObstacles
        scope.launch { saveObstacles(context, newObstacles) }
        if (endPoint != null) computePath()
        Toast.makeText(context, "Препятствие добавлено", Toast.LENGTH_SHORT).show()
    }

    fun removeObstacleAt(point: dataMap) {
        val mat = PUBLICMATRIX.value ?: return
        val toRemove = obstacles.find { obs ->
            val dx = obs.x - point.x
            val dy = obs.y - point.y
            dx*dx + dy*dy <= 100
        } ?: return
        val radius = 5
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx*dx + dy*dy <= radius*radius) {
                    val x = toRemove.x + dx
                    val y = toRemove.y + dy
                    if (x in 0 until mat.width && y in 0 until mat.height) {
                        val original = PUBLICMATRIX.value ?: continue
                        val isWalkable = original.get(x, y)
                        if (isWalkable) {
                            PUBLICMATRIX.value?.set(x, y, true)
                        }
                    }
                }
            }
        }
        val newObstacles = obstacles - toRemove
        obstacles = newObstacles
        scope.launch { saveObstacles(context, newObstacles) }
        if (endPoint != null) computePath()
        Toast.makeText(context, "Препятствие удалено", Toast.LENGTH_SHORT).show()
    }

    fun resetAll() {
        startPoint = null
        endPoint = null
        pathPoints = emptyList()
        isSelectingStart = false
        isObstacleMode = false
    }

    LaunchedEffect(Unit) {
        val matrix = initBitMatrix(context)
        PUBLICMATRIX.set(matrix)
        val loaded = loadObstacles(context)
        obstacles = loaded
        loaded.forEach { PUBLICMATRIX.addObstruction(it.x, it.y) }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(
            pathPoints = pathPoints,
            startPoint = startPoint,
            endPoint = endPoint,
            pointsOfInterest = pointsOfInterest,
            obstacles = obstacles,
            onPointOfInterestTap = { poi ->
                if (isSelectingStart) {
                    startPoint = poi.pos
                    endPoint = null
                    pathPoints = emptyList()
                    isSelectingStart = false
                } else if (startPoint != null && endPoint == null) {
                    endPoint = poi.pos
                    pathPoints = emptyList()
                    computePath()
                } else {
                    selectedPoi = poi
                    showBottomDialog = true
                }
            },
            onTap = { point ->
                when {
                    isObstacleMode -> addObstacle(point)
                    isSelectingStart -> {
                        startPoint = point
                        endPoint = null
                        pathPoints = emptyList()
                        isSelectingStart = false
                    }
                    startPoint != null && endPoint == null -> {
                        endPoint = point
                        pathPoints = emptyList()
                        computePath()
                    }
                    else -> { }
                }
            },
            onDoubleTap = { resetAll() },
            onLongTap = { point ->
                if (!isObstacleMode) {
                    removeObstacleAt(point)
                }
            }
        )

        Row(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            FloatingActionButton(
                onClick = {
                    when {
                        startPoint == null && !isSelectingStart -> isSelectingStart = true
                        !(startPoint != null && endPoint == null) -> {
                            resetAll()
                            isSelectingStart = true
                        }
                    }
                },
                containerColor = Color(0xFF3E55A2),
                contentColor = Color.White,
                modifier = Modifier
                    .width(80.dp)
                    .height(48.dp)
            ) {
                Text("От/До", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { isObstacleMode = !isObstacleMode },
                containerColor = if (isObstacleMode) Color(0xFFFF5722) else Color(0xFF9E9E9E),
                contentColor = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Text("-", fontSize = 24.sp)
            }
        }
    }

    if (showBottomDialog && selectedPoi != null) {
        val poi = selectedPoi!!
        val hoursRaw = shopHoursMap[poi.id] ?: ""
        val hoursDisplay = if (hoursRaw.isBlank()) "Не указано" else hoursRaw

        androidx.compose.ui.window.Popup(
            onDismissRequest = { showBottomDialog = false },
            properties = androidx.compose.ui.window.PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = { showBottomDialog = false },
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(poi.name, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Часы работы: $hoursDisplay", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Пустышка", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Тестовая кнопка")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}