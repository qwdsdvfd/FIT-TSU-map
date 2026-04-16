package com.example.tsumap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.algorithm.AI

@Composable
fun RatingDialog(
    shopName: String,
    matrix: List<List<Boolean>>,
    onToggleCell: (Int, Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    recognizer: AI
) {
    var predictedDigit by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(matrix) {
        val (digit) = recognizer.predict(matrix)
        predictedDigit = digit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Оцените: $shopName", fontSize = 18.sp)
                Text("Нарисуйте цифру от 0 до 9", fontSize = 14.sp, color = Color.Gray)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    Column {
                        for (i in 0 until 5) {
                            Row {
                                for (j in 0 until 5) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .background(
                                                if (matrix[i][j]) Color.Black else Color.White,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { onToggleCell(i, j) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onToggleCell(-1, -1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Очистить") }
                }

                Spacer(modifier = Modifier.height(8.dp))

            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                Button(
                    onClick = { predictedDigit?.let { onConfirm(it) } },
                    enabled = predictedDigit != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Подтвердить оценку ${predictedDigit ?: ""}") }
            }
        }
    )
}
