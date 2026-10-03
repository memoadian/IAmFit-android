package com.geeckosoft.iamfit.ui.screens.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geeckosoft.iamfit.BuildConfig
import com.geeckosoft.iamfit.data.ApiException
import com.geeckosoft.iamfit.ui.components.SectionTitle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface CheckState {
    data object Idle : CheckState
    data object Loading : CheckState
    data class Ok(val reply: String) : CheckState
    data class Error(val message: String) : CheckState
}

private fun goalLabel(goal: String?): String = when (goal) {
    "lose" -> "Bajar grasa"
    "gain" -> "Ganar músculo"
    "maintain" -> "Mantener"
    else -> "Sin objetivo"
}

private fun activityLabel(level: String?): String = when (level) {
    "sedentary" -> "Sedentario"
    "light" -> "Ligero"
    "moderate" -> "Moderado"
    "active" -> "Activo"
    "very_active" -> "Muy activo"
    else -> "—"
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }
    val state = viewModel.state

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        state.name.ifBlank { "Tu perfil" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.error ?: "${goalLabel(state.goal)} · ${activityLabel(state.activityLevel)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.error == null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        SectionTitle("Datos")
                        Spacer(Modifier.height(12.dp))
                        ProfileRow("Correo", state.email.ifBlank { "—" })
                        ProfileRow(
                            "Estatura",
                            state.heightCm?.let { "${(it * 10).roundToInt() / 10f} cm" } ?: "—",
                        )
                        ProfileRow(
                            "Peso actual",
                            state.latestWeightKg?.let { "${(it * 10).roundToInt() / 10f} kg" } ?: "—",
                        )
                        ProfileRow("Zona horaria", state.timezone ?: "—")
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
                Column {
                    SettingsRow(icon = Icons.Rounded.RestaurantMenu, label = "Preferencias de nutrición")
                    Divider()
                    SettingsRow(icon = Icons.Rounded.NotificationsNone, label = "Notificaciones")
                    Divider()
                    SettingsRow(icon = Icons.Rounded.Shield, label = "Privacidad")
                    Divider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        label = "Cerrar sesión",
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                    )
                }
            }
        }

        item { SectionTitle("Diagnóstico", subtitle = "Herramientas para desarrollo") }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Conexión con el backend", style = MaterialTheme.typography.titleMedium)
                            Text(
                                BuildConfig.API_BASE_URL,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            checkState = CheckState.Loading
                            scope.launch {
                                checkState = try {
                                    val result = viewModel.checkHealth()
                                    CheckState.Ok(result.status)
                                } catch (e: ApiException) {
                                    CheckState.Error(e.userMessage)
                                } catch (e: Exception) {
                                    CheckState.Error("Error inesperado: ${e.message}")
                                }
                            }
                        },
                        enabled = checkState !is CheckState.Loading,
                    ) {
                        Text("Probar conexión")
                    }
                    Spacer(Modifier.height(12.dp))
                    AnimatedContent(targetState = checkState, label = "checkState", transitionSpec = { fadeIn() togetherWith fadeOut() }) { check ->
                        when (check) {
                            CheckState.Idle -> Spacer(Modifier.height(1.dp))
                            CheckState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            is CheckState.Ok -> Text("✅ Conectado — estado: \"${check.reply}\"", style = MaterialTheme.typography.bodyMedium)
                            is CheckState.Error -> Text(
                                "❌ ${check.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Divider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {}
}
