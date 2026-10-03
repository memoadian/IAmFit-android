package com.geeckosoft.iamfit.ui.screens.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geeckosoft.iamfit.data.ApiException
import com.geeckosoft.iamfit.data.IamFitRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class UiMeal(
    val title: String,
    val subtitle: String,
    val kcal: Int,
    val meal: String,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Ready(
        val userName: String,
        val consumedKcal: Int,
        val targetKcal: Int,
        val proteinG: Int,
        val proteinTargetG: Int,
        val carbG: Int,
        val carbTargetG: Int,
        val fatG: Int,
        val fatTargetG: Int,
        val meals: List<UiMeal>,
        val hasTarget: Boolean,
    ) : HomeUiState
}

private val MEAL_ORDER = listOf("breakfast", "lunch", "dinner", "snack")

/**
 * Resumen del día: combina `GET /energy` (meta de kcal y macros) con
 * `GET /diary` (consumido y comidas registradas).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = IamFitRepository(application)

    var state by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state = HomeUiState.Loading

            state = try {
                val user = repo.me()
                // /energy responde 422 si aún no hay perfil/peso: no es un error fatal.
                val energy = runCatching { repo.energy() }.getOrNull()
                val diary = repo.diary()
                val target = diary.target

                HomeUiState.Ready(
                    userName = user.name,
                    consumedKcal = diary.totals.kcal.roundToInt(),
                    targetKcal = (target?.kcal ?: energy?.targetKcal ?: 0.0).roundToInt(),
                    proteinG = diary.totals.proteinG.roundToInt(),
                    proteinTargetG = target?.macros?.proteinG ?: energy?.macros?.proteinG ?: 0,
                    carbG = diary.totals.carbG.roundToInt(),
                    carbTargetG = target?.macros?.carbG ?: energy?.macros?.carbG ?: 0,
                    fatG = diary.totals.fatG.roundToInt(),
                    fatTargetG = target?.macros?.fatG ?: energy?.macros?.fatG ?: 0,
                    meals = MEAL_ORDER.flatMap { meal ->
                        (diary.entries[meal] ?: emptyList()).map { entry ->
                            UiMeal(
                                title = entry.food?.name ?: "Alimento",
                                subtitle = "${entry.grams.roundToInt()} g",
                                kcal = entry.kcal.roundToInt(),
                                meal = meal,
                            )
                        }
                    },
                    hasTarget = target != null || energy != null,
                )
            } catch (e: ApiException) {
                HomeUiState.Error(e.userMessage)
            } catch (e: Exception) {
                HomeUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
}
