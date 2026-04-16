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
import com.example.tsumap.algorithm.AI
import com.example.tsumap.algorithm.PUBLICMATRIX
import com.example.tsumap.algorithm.dataMap
import com.example.tsumap.data.*
import com.example.tsumap.navigation.NavigationManager
import com.example.tsumap.ui.components.AttractionSelectorDialog
import com.example.tsumap.ui.components.MapFromAssets
import com.example.tsumap.ui.components.RatingDialog
import com.example.tsumap.ui.theme.TSUMAPTheme
import com.example.tsumap.utils.initBitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val digitRecognizer = remember { AI() }

    var startPoint by remember { mutableStateOf<dataMap?>(null) }
    var endPoint by remember { mutableStateOf<dataMap?>(null) }
    var pathPoints by remember { mutableStateOf<List<dataMap>>(emptyList()) }
    var obstacles by remember { mutableStateOf<List<dataMap>>(emptyList()) }

    var isInitializing by remember { mutableStateOf(true) }
    var initMessage by remember { mutableStateOf("Загрузка карты...") }

    LaunchedEffect(Unit) {
        launch { navManager.start.collect { startPoint = it } }
        launch { navManager.end.collect { endPoint = it } }
        launch { navManager.path.collect { pathPoints = it } }
        launch { navManager.obstacles.collect { obstacles = it } }
    }

    var isSelectingStart by remember { mutableStateOf(false) }
    var isObstacleMode by remember { mutableStateOf(false) }
    var showAttractionSelector by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<pointOfInterest?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var matrix5x5 by remember { mutableStateOf(List(5) { List(5) { false } }) }
    var shopRatings by remember { mutableStateOf<Map<Int, ShopRating>>(emptyMap()) }
    var showAIDialog by remember { mutableStateOf(false) }

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
        try {
            val matrix = withContext(Dispatchers.IO) {
                initBitMatrix(context)
            }

            if (matrix == null) {
                Toast.makeText(context, "Ошибка загрузки карты проходимости", Toast.LENGTH_LONG).show()
                isInitializing = false
                return@LaunchedEffect
            }

            PUBLICMATRIX.set(matrix)
            initMessage = "Загрузка рейтингов..."

            shopRatings = withContext(Dispatchers.IO) {
                RatingStorage.getAllRatings(context)
            }

            initMessage = "Загрузка нейросети..."

            val needsTraining = withContext(Dispatchers.IO) {
                !digitRecognizer.loadWeights(context)
            }

            if (needsTraining) {
                withContext(Dispatchers.Main) {
                    initMessage = "Обучение ИИ... 0%"
                }

                withContext(Dispatchers.IO) {
                    digitRecognizer.quickTrain(context)
                    digitRecognizer.saveWeights(context)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "ИИ готов к работе!", Toast.LENGTH_SHORT).show()
                }
            }

            if (startPoint == null) {
                navManager.setStart(dataMap(1420, 2310))
            }

            navManager.onMatrixReady()
            isInitializing = false

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
            isInitializing = false
        }
    }

    fun snapToNearestPoi(point: dataMap, pois: List<pointOfInterest>, radius: Int = 100): dataMap {
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
        if (isInitializing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF3E55A2)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = initMessage,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
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
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { showAIDialog = true },
                    containerColor = Color(0xFF9C27B0),
                    modifier = Modifier.size(48.dp)
                ) { Text("ИИ", fontSize = 12.sp) }
            }
        }
    }

    if (selectedPoi != null) {
        val poi = selectedPoi!!
        val currentRating = shopRatings[poi.id]

        BottomSheetDialog(
            onDismiss = { selectedPoi = null },
            title = poi.name,
            text = "Часы работы: ${shopHoursMap[poi.id] ?: "Не указано"}",
            rating = currentRating,
            onFeedbackClick = {
                selectedPoi = null
                showRatingDialog = true
            }
        )
    }

    if (showRatingDialog && selectedPoi != null) {
        val poi = selectedPoi!!
        RatingDialog(
            shopName = poi.name,
            matrix = matrix5x5,
            onToggleCell = { r, c ->
                if (r == -1 && c == -1) {
                    matrix5x5 = List(5) { List(5) { false } }
                } else {
                    matrix5x5 = matrix5x5.mapIndexed { i, row ->
                        if (i == r) row.mapIndexed { j, cell -> if (j == c) !cell else cell }
                        else row
                    }
                }
            },
            onConfirm = { rating ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        RatingStorage.saveRating(context, poi.id, rating)
                        digitRecognizer.train(matrix5x5, rating)
                        digitRecognizer.saveWeights(context)
                    }

                    shopRatings = withContext(Dispatchers.IO) {
                        RatingStorage.getAllRatings(context)
                    }

                    Toast.makeText(context, "Спасибо за оценку $rating!", Toast.LENGTH_LONG).show()
                    showRatingDialog = false
                    matrix5x5 = List(5) { List(5) { false } }
                }
            },
            onDismiss = {
                showRatingDialog = false
                selectedPoi = null
            },
            recognizer = digitRecognizer
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

    if (showAIDialog) {
        var isTraining by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0) }
        var statusMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isTraining) showAIDialog = false },
            title = { Text("Управление нейронной сетью") },
            text = {
                Column {
                    Text("Статус: ${if (digitRecognizer.isReady()) "Обучена" else "Не обучена"}")
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isTraining) {
                        Text("Прогресс обучения:", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$progress% - $statusMessage", fontSize = 14.sp, color = Color(0xFF1976D2))
                    } else {
                        Text("Вы можете сбросить веса и переобучить сеть, если она плохо распознает цифры.")
                    }
                }
            },
            confirmButton = {
                if (!isTraining) {
                    Row {
                        TextButton(onClick = { showAIDialog = false }) { Text("Закрыть") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isTraining = true
                                progress = 0
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        digitRecognizer.resetAndRetrain(context) { status ->
                                            if (status.contains("Обучение:")) {
                                                progress = status.replace("Обучение:", "").replace("%", "").trim().toIntOrNull() ?: progress
                                            }
                                            statusMessage = status
                                        }
                                    }
                                    progress = 100
                                    Toast.makeText(context, "ИИ переобучен!", Toast.LENGTH_LONG).show()
                                    isTraining = false
                                    showAIDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) { Text("Сбросить и переобучить") }
                    }
                } else {
                    TextButton(onClick = { }, enabled = false) { Text("Идет обучение...") }
                }
            }
        )
    }
}

@Composable
fun BottomSheetDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String,
    rating: ShopRating?,
    onFeedbackClick: () -> Unit
) {
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text, fontSize = 14.sp)

                    if (rating != null && rating.count > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Рейтинг: ", fontSize = 14.sp)
                            Text(
                                "${String.format("%.1f", rating.average)}/10",
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Text(" (${rating.count} отзывов)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFeedbackClick, modifier = Modifier.fillMaxWidth()) {
                        Text(if (rating != null && rating.count > 0) "Изменить оценку" else "Оценить заведение")
                    }
                }
            }
        }
    }
}