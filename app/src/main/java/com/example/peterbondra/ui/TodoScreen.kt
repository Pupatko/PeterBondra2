package com.example.peterbondra.ui

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.peterbondra.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TodoScreen(
    tasks: List<Task>,
    onMarkDone: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No active tasks yet. Tap + to create one.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val doneSwipeGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF16492E),
            Color(0xFF1E6A41),
            Color(0xFF2A8A55),
        ),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = tasks, key = { it.id }) { task ->
            val holdScope = rememberCoroutineScope()
            var holdJob by remember(task.id) { mutableStateOf<Job?>(null) }
            var showIntensity by remember(task.id) { mutableStateOf(false) }
            var downX by remember(task.id) { mutableFloatStateOf(0f) }
            var downY by remember(task.id) { mutableFloatStateOf(0f) }

            DisposableEffect(task.id) {
                onDispose {
                    holdJob?.cancel()
                }
            }

            val animatedProgress = animateFloatAsState(
                targetValue = task.intensity / 100f,
                label = "intensityProgress",
            )

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                        onMarkDone(task)
                        true
                    } else {
                        false
                    }
                },
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.5.dp)
                            .clip(TaskCardShape)
                            .background(doneSwipeGradient)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "MARK AS DONE",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                },
            ) {
                TaskCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .pointerInteropFilter { motionEvent ->
                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = motionEvent.x
                                    downY = motionEvent.y
                                    holdJob?.cancel()
                                    holdJob = holdScope.launch {
                                        delay(280)
                                        showIntensity = true
                                    }
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val movedX = abs(motionEvent.x - downX)
                                    val movedY = abs(motionEvent.y - downY)
                                    if (movedX > 24f || movedY > 24f) {
                                        holdJob?.cancel()
                                    }
                                }

                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> {
                                    holdJob?.cancel()
                                    showIntensity = false
                                }
                            }
                            false
                        },
                ) {
                    Text(
                        text = task.text,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )

                    AnimatedVisibility(visible = showIntensity) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Intensity ${task.intensity}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            LinearProgressIndicator(
                                progress = { animatedProgress.value },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
