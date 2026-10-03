package com.geeckosoft.iamfit.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geeckosoft.iamfit.ui.components.MacroBar
import com.geeckosoft.iamfit.ui.components.ProgressRing
import com.geeckosoft.iamfit.ui.components.SectionTitle

private fun mealIcon(meal: String): ImageVector = when (meal) {
    "breakfast" -> Icons.Rounded.FreeBreakfast
    "lunch" -> Icons.Rounded.LocalDining
    "dinner" -> Icons.Rounded.Restaurant
    else -> Icons.Rounded.Egg
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel()) {
    when (val state = viewModel.state) {
        is HomeUiState.Loading -> LoadingState(modifier)
        is HomeUiState.Error -> ErrorState(state.message, viewModel::refresh, modifier)
        is HomeUiState.Ready -> HomeContent(state, viewModel::refresh, modifier)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (state.targetKcal > 0) {
        state.consumedKcal / state.targetKcal.toFloat()
    } else {
        0f
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 3 },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Hola, ${state.userName.substringBefore(' ')} 👋",
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Así va tu día",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar")
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500, delayMillis = 80)) + slideInVertically(tween(500, delayMillis = 80)) { it / 4 },
            ) {
                CalorieCard(
                    consumedKcal = state.consumedKcal,
                    targetKcal = state.targetKcal,
                    progress = progress,
                )
            }
        }

        if (!state.hasTarget) {
            item {
                InfoCard("Completa tu perfil y registra tu peso para calcular tu meta de calorías y macros.")
            }
        }

        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500, delayMillis = 160)) + slideInVertically(tween(500, delayMillis = 160)) { it / 4 },
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        SectionTitle("Macros de hoy")
                        Spacer(Modifier.height(16.dp))
                        MacroBar("Proteína", state.proteinG, state.proteinTargetG, Color(0xFF12B76A))
                        Spacer(Modifier.height(14.dp))
                        MacroBar("Carbohidratos", state.carbG, state.carbTargetG, Color(0xFF3AA0FF))
                        Spacer(Modifier.height(14.dp))
                        MacroBar("Grasas", state.fatG, state.fatTargetG, Color(0xFFFF6B4A))
                    }
                }
            }
        }

        item {
            SectionTitle("Comidas registradas", subtitle = "${state.meals.size} entradas hoy")
        }

        if (state.meals.isEmpty()) {
            item { InfoCard("Aún no registras comidas hoy.") }
        } else {
            items(state.meals) { meal ->
                MealRow(meal)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun CalorieCard(consumedKcal: Int, targetKcal: Int, progress: Float) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(progress = progress, ringSize = 128.dp, strokeWidth = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "$consumedKcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Meta diaria", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (targetKcal > 0) "$targetKcal kcal" else "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (targetKcal > 0) {
                        "Te quedan ${(targetKcal - consumedKcal).coerceAtLeast(0)} kcal"
                    } else {
                        "Sin meta calculada"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MealRow(meal: UiMeal) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(mealIcon(meal.meal), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.title, style = MaterialTheme.typography.titleMedium)
                Text(meal.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "${meal.kcal} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
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
