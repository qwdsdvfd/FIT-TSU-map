package com.example.tsumap

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun MapFromAssets(fileName: String) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(fileName) {
        val bitmap = context.assets.open(fileName).use { input ->
            BitmapFactory.decodeStream(input)
        }
        imageBitmap = bitmap?.asImageBitmap()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = "Карта",
            modifier = Modifier.fillMaxSize()
        )
    }
}