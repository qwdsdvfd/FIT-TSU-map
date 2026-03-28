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
    var statusMessage by remember { mutableStateOf("Нажмите От/До") }
    var statusColor by remember { mutableStateOf(Color(0xFF333333)) }
    var isSelectingStart by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val matrix = initBitMatrix(context)
        PUBLICMATRIX.set(matrix)
    }

    fun computePath() {
        val mat = PUBLICMATRIX.value
        if (mat == null) return
        val start = startPoint ?: return
        val end = endPoint ?: return
        statusMessage = "Поиск пути..."
        statusColor = Color(0xFF757575)
        val result = aStar(mat, start, end)
        if (result != null) {
            pathPoints = result
            statusMessage = "Маршрут найден"
            statusColor = Color(0xFF4CAF50)
        }
        else {
            pathPoints = emptyList()
            statusMessage = "Путь не существует"
            statusColor = Color(0xFFF44336)
        }
    }

    fun resetAll() {
        startPoint = null
        endPoint = null
        pathPoints = emptyList()
        isSelectingStart = false
        statusMessage = "Нажмите От/До и выберите точку старта"
        statusColor = Color(0xFF606060)
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(
            pathPoints = pathPoints,
            startPoint = startPoint,
            endPoint = endPoint,
            onTap = { point ->
                when {
                    isSelectingStart -> {
                        startPoint = point
                        endPoint = null
                        pathPoints = emptyList()
                        isSelectingStart = false
                        statusMessage = "Старт выбран,  нажмите для выбора цели"
                        statusColor = Color(0xFF606060)
                    }
                    startPoint != null && endPoint == null -> {
                        endPoint = point
                        pathPoints = emptyList()
                        computePath()
                    }
                    else -> {
                        statusMessage = "Двойной тап для сброса"
                        statusColor = Color(0xFF606060)
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
                        statusMessage = "Нажмите на карту, чтобы выбрать точку старта"
                        statusColor = Color(0xFF606060)
                    }
                    startPoint != null && endPoint == null -> {
                        statusMessage = "Нажмите на карту для выбора цели"
                        statusColor = Color(0xFF606060)
                    }
                    else -> {
                        resetAll()
                        isSelectingStart = true
                        statusMessage = "Нажмите на карту, чтобы выбрать старт"
                        statusColor = Color(0xFF606060)
                    }
                }
            },
            containerColor = Color(0xFF4CAF50),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        {
            Text("От/До", fontSize = 24.sp)
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            color = statusColor,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
        {
            Text(
                text = statusMessage,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}