package com.example.tsumap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TSUMAPApp()
        }
    }
}

@PreviewScreenSizes
@Composable
fun TSUMAPApp() {
    var fileName by remember { mutableStateOf("map_walk.png") }  // текущий файл
    val context = LocalContext.current

    initBitMatrix(context)
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        MapFromAssets(fileName)  // передаём имя файла

        FloatingActionButton(
            onClick = {
                // переключаем между двумя картинками
                fileName = if (fileName == "map_walk.png") "skeleton.png" else "map_walk.png"
            },
            containerColor = Color(127, 199, 255),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = "Изменить")
        }
    }
}
