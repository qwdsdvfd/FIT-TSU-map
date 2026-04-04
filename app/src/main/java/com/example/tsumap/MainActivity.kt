package com.example.tsumap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.ui.theme.TSUMAPTheme

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
    var startPoint by remember { mutableStateOf<dataMap?>(null) }
    var endPoint by remember { mutableStateOf<dataMap?>(null) }
    var pathPoints by remember { mutableStateOf<List<dataMap>>(emptyList()) }
    var isSelectingStart by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<pointOfInterest?>(null) }
    var showPoiDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Загрузка точек из assets
    val pointsOfInterest = remember {
        runCatching {
            context.assets.open("База_данных_магазинов - Магазины.csv")
                .bufferedReader()
                .use { parsePointOfInterest(it.readText()) }
        }.getOrElse { emptyList() }
    }

    LaunchedEffect(Unit) {
        val matrix = initBitMatrix(context)
        PUBLICMATRIX.set(matrix)
    }

    fun computePath() {
        val mat = PUBLICMATRIX.value ?: return
        val start = startPoint ?: return
        val end = endPoint ?: return
        val result = aStar(mat, start, end)
        if (result != null) {
            pathPoints = result
        } else {
            pathPoints = emptyList()
        }
    }

    fun resetAll() {
        startPoint = null
        endPoint = null
        pathPoints = emptyList()
        isSelectingStart = false
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(
            pathPoints = pathPoints,
            startPoint = startPoint,
            endPoint = endPoint,
            pointsOfInterest = pointsOfInterest,
            onPointOfInterestTap = { pointOfInterest ->
                when {
                    isSelectingStart -> {
                        startPoint = pointOfInterest.pos
                        endPoint = null
                        pathPoints = emptyList()
                        isSelectingStart = false
                    }
                    startPoint != null && endPoint == null -> {
                        endPoint = pointOfInterest.pos
                        pathPoints = emptyList()
                        computePath()
                    }
                    else -> {
                        resetAll()
                        isSelectingStart = true
                    }
                }
            },
            onTap = { point ->
                when {
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
                    else -> {
                    }
                }
            },
            onDoubleTap = { resetAll() }
        )

        FloatingActionButton(
            onClick = {
                when {
                    startPoint == null && !isSelectingStart -> {
                        isSelectingStart = true
                    }
                    !(startPoint != null && endPoint == null) -> {
                        resetAll()
                        isSelectingStart = true
                    }
                }
            },
            containerColor = Color(0xFF4169E1),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
        { Text("От/До", fontSize = 24.sp) }
    }
}