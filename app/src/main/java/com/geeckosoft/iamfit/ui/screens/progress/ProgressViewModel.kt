package com.geeckosoft.iamfit.ui.screens.progress

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geeckosoft.iamfit.data.ApiException
import com.geeckosoft.iamfit.data.IamFitRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface ProgressUiState {
    data object Loading : ProgressUiState
    data class Error(val message: String) : ProgressUiState
    data class Ready(
        val weights: List<Float>,
        val weightLabels: List<String>,
        val latestKg: Float?,
        val streakDays: List<Boolean>,
        val streakLabels: List<String>,
        val currentStreak: Int,
    ) : ProgressUiState
}

/**
 * Peso (GET /weight?days=7) y racha (GET /progress/streak) del usuario.
 */
class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = IamFitRepository(application)

    var state by mutableStateOf<ProgressUiState>(ProgressUiState.Loading)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state = ProgressUiState.Loading

            state = try {
                val weights = repo.weights(7).sortedBy { it.measuredOn }
                val streak = repo.streak()

                ProgressUiState.Ready(
                    weights = weights.map { it.weightKg.toFloat() },
                    weightLabels = weights.map { labelFor(it.measuredOn) },
                    latestKg = weights.lastOrNull()?.weightKg?.toFloat(),
                    streakDays = streak.days.map { it.completed },
                    streakLabels = streak.days.map { labelFor(it.date) },
                    currentStreak = streak.currentStreak,
                )
            } catch (e: ApiException) {
                ProgressUiState.Error(e.userMessage)
            } catch (e: Exception) {
                ProgressUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
}

/** Inicial del día en español a partir de una fecha ISO (YYYY-MM-DD). */
internal fun labelFor(isoDate: String): String = try {
    when (LocalDate.parse(isoDate).dayOfWeek) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "M"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
} catch (e: Exception) {
    ""
}
