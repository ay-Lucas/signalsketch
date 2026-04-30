package com.example.signalsketch.ui.mapping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.viewmodel.FloorplanRoomBox

@Composable
fun FloorplanBuilderCard(
    boxes: List<FloorplanRoomBox>,
    selectedBoxId: Long?,
    onAddBox: (String) -> Unit,
    onUpdateBoxLabel: (Long, String) -> Unit,
    onSelectBox: (Long?) -> Unit,
    onRemoveBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Floorplan Builder",
    description: String = "Add labeled room boxes over the live heatmap. Tap a box on the map to select it, drag to move it, and use two fingers to resize or reposition it."
) {
    var newLabel by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark,
            contentColor = OnDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("Room label") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        onAddBox(newLabel)
                        newLabel = ""
                    }
                ) {
                    Text("Add")
                }
            }

            if (boxes.isEmpty()) {
                Text(
                    text = "No room boxes yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    boxes.forEachIndexed { index, box ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = if (box.id == selectedBoxId) 2.dp else 1.dp,
                                color = if (box.id == selectedBoxId) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    androidx.compose.ui.graphics.Color(box.colorArgb).copy(alpha = 0.75f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = androidx.compose.ui.graphics.Color(box.colorArgb),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = "Room ${index + 1}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedTextField(
                                    value = box.label,
                                    onValueChange = { onUpdateBoxLabel(box.id, it) },
                                    label = { Text("Label") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = { onSelectBox(box.id) }) {
                                        Text(if (box.id == selectedBoxId) "Selected" else "Select")
                                    }
                                    OutlinedButton(onClick = { onRemoveBox(box.id) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                            Text(
                                text = "Size ${box.widthMeters.format(1)}m x ${box.heightMeters.format(1)}m  |  Center ${box.centerXMeters.format(1)}, ${box.centerYMeters.format(1)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun Float.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
