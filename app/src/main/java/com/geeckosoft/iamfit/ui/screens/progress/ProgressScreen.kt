package com.geeckosoft.iamfit.ui.screens.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geeckosoft.iamfit.ui.components.SectionTitle
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(modifier: Modifier = Modifier, viewModel: ProgressViewModel = viewModel()) {
    when (val state = viewModel.state) {
        is ProgressUiState.Loading -> LoadingState(modifier)
        is ProgressUiState.Error -> ErrorState(state.message, viewModel::refresh, modifier)
        is ProgressUiState.Ready -> ProgressContent(state, modifier)
    }
}

@Composable
private fun ProgressContent(state: ProgressUiState.Ready, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { SectionTitle("Tu progreso", subtitle = "Últimos 7 días") }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Peso corporal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.latestKg?.let { "${(it * 10).roundToInt() / 10f} kg" } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    if (state.weights.size >= 2) {
                        WeightTrendChart(
                            values = state.weights,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            state.weightLabels.forEach {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            "Registra al menos dos pesos para ver la tendencia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Racha semanal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.currentStreak} ${if (state.currentStreak == 1) "día" else "días"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (state.streakDays.isEmpty()) {
                            Text(
                                "Sin registros todavía.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.streakDays.forEachIndexed { index, done ->
                                StreakDot(
                                    day = state.streakLabels.getOrElse(index) { "" },
                                    done = done,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun WeightTrendChart(values: List<Float>, modifier: Modifier = Modifier) {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(values) { animation.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessLow)) }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            val x = stepX * index
            val normalized = (value - minVal) / range
            val y = size.height - (normalized * size.height * 0.8f) - size.height * 0.1f
            Offset(x, y)
        }

        val animatedCount = (points.size * animation.value).toInt().coerceIn(1, points.size)
        val visiblePoints = points.take(animatedCount)

        for (i in 0 until visiblePoints.size - 1) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(primary, tertiary)),
                start = visiblePoints[i],
                end = visiblePoints[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }
        visiblePoints.forEach { point ->
            drawCircle(color = primary, radius = 6f, center = point)
            drawCircle(color = Color.White, radius = 2.5f, center = point)
        }
    }
}

@Composable
private fun StreakDot(day: String, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(34.dp),
        ) {}
        Spacer(Modifier.height(6.dp))
        Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("❌ $message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
