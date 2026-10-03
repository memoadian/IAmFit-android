package com.geeckosoft.iamfit.ui.screens.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geeckosoft.iamfit.data.ApiException
import com.geeckosoft.iamfit.data.IamFitRepository
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
    data object Success : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = IamFitRepository(application)

    var state by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(email: String, password: String) = submit { repo.login(email, password) }

    fun register(name: String, email: String, password: String) =
        submit { repo.register(name, email, password) }

    fun resetError() {
        if (state is AuthUiState.Error) state = AuthUiState.Idle
    }

    private fun submit(block: suspend () -> Unit) {
        if (state is AuthUiState.Loading) return

        state = AuthUiState.Loading

        viewModelScope.launch {
            state = try {
                block()
                AuthUiState.Success
            } catch (e: ApiException) {
                AuthUiState.Error(e.userMessage)
            } catch (e: Exception) {
                AuthUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
}
