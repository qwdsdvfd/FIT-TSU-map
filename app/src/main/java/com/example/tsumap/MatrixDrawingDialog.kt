package com.example.tsumap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MatrixDrawingDialog(
    matrix: List<List<Boolean>>,
    onToggleCell: (row: Int, col: Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Рисование матрицы 5x5", fontSize = 20.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 5) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (col in 0 until 5) {
                            val isFilled = matrix[row][col]
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(2.dp)
                                    .background(
                                        color = if (isFilled) Color(0xFF3E55A2) else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onToggleCell(row, col) }
                            )
                        }
                    }
                    if (row < 4) Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Нажмите на ячейку для изменения", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}