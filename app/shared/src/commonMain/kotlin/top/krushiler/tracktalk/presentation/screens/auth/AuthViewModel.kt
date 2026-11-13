package top.krushiler.tracktalk.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.repository.AuthRepository

data class AuthUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<AuthUiState>
        field = MutableStateFlow(AuthUiState())

    fun onLoginChange(value: String) {
        uiState.update { it.copy(login = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        val login = uiState.value.login.trim()
        val password = uiState.value.password
        if (login.isEmpty() || password.isEmpty() || uiState.value.isLoading) return

        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.login(login, password) }.onSuccess {
                uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { error ->
                uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Login failed",
                    )
                }
            }
        }
    }
}
