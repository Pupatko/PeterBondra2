package com.example.peterbondra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onSave: (text: String, intensity: Int) -> Unit,
) {
    var taskText by rememberSaveable { mutableStateOf("") }
    var intensity by rememberSaveable { mutableFloatStateOf(40f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What do you want to do?") },
                    singleLine = true,
                )

                Text("How much energy should I spam you with to get this done? 0% = chill once a day, 100% = every 5 minutes non-stop.")

                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0f..100f,
                    steps = 99,
                )

                Text("Intensity: ${intensity.roundToInt()}%")
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = taskText.isNotBlank(),
                onClick = {
                    onSave(taskText.trim(), intensity.roundToInt())
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
