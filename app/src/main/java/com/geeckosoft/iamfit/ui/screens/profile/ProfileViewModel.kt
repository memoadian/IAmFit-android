package com.geeckosoft.iamfit.ui.screens.profile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geeckosoft.iamfit.data.ApiException
import com.geeckosoft.iamfit.data.HealthDto
import com.geeckosoft.iamfit.data.IamFitRepository
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val goal: String? = null,
    val activityLevel: String? = null,
    val heightCm: Double? = null,
    val timezone: String? = null,
    val latestWeightKg: Double? = null,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = IamFitRepository(application)

    var state by mutableStateOf(ProfileUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state = ProfileUiState(loading = true)

            state = try {
                val user = repo.me()
                val profile = repo.profile()

                ProfileUiState(
                    loading = false,
                    name = user.name,
                    email = user.email,
                    goal = profile.profile?.goal,
                    activityLevel = profile.profile?.activityLevel,
                    heightCm = profile.profile?.heightCm,
                    timezone = profile.profile?.timezone,
                    latestWeightKg = profile.latestWeightKg,
                )
            } catch (e: ApiException) {
                ProfileUiState(loading = false, error = e.userMessage)
            } catch (e: Exception) {
                ProfileUiState(loading = false, error = "Error inesperado: ${e.message}")
            }
        }
    }

    suspend fun checkHealth(): HealthDto = repo.health()

    fun logout() = repo.logout()
}
