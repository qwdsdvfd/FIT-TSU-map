package com.example.tsumap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumap.data.Attraction

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
                Text("Найдено: ${attractions.size}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(attractions) { attraction ->
                        val checked = attraction.id in selectedIds
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val new = selectedIds.toMutableSet()
                                if (checked) new.remove(attraction.id) else new.add(attraction.id)
                                onSelectionChanged(new)
                            }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                val new = selectedIds.toMutableSet()
                                if (isChecked) new.add(attraction.id) else new.remove(attraction.id)
                                onSelectionChanged(new)
                            })
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(attraction.name, fontSize = 14.sp)
                                Text("Рейтинг: ${attraction.rating}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRun, enabled = !isRunning && selectedIds.isNotEmpty()) {
                if (isRunning) Text("Загрузка...") else Text("Построить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isRunning) { Text("Отмена") } }
    )
}