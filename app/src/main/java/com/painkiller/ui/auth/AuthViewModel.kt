package com.painkiller.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.painkiller.data.github.GithubAuthRepository
import com.painkiller.data.github.GithubAuthResult
import com.painkiller.data.github.GithubAuthState
import com.painkiller.data.github.GithubTokenFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the auth screen's state machine.
 *
 * The user pastes a Personal Access Token, taps "Sign in", and the VM:
 *   1. quick-checks the format ([GithubTokenFormat.looksValid]) for an
 *      instant client-side hint
 *   2. asks [GithubAuthRepository.signInWithPersonalAccessToken] to validate
 *      the token against `GET /user` and store it on success
 *   3. emits a new [AuthUiState] reflecting either authentication or a
 *      human-readable failure reason
 *
 * The raw token is held only in [AuthUiState.tokenInput] while the user is
 * editing; it is wiped from state as soon as sign-in completes (success or
 * failure).
 */
class AuthViewModel(
    private val authRepository: GithubAuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        refreshAuthState()
    }

    fun onTokenChanged(value: String) {
        _state.update {
            it.copy(
                tokenInput = value,
                formatLooksValid = GithubTokenFormat.looksValid(value),
                errorMessage = null,
            )
        }
    }

    fun signIn() {
        val current = _state.value
        if (current.isSubmitting) return
        val token = current.tokenInput.trim()
        if (token.isEmpty()) {
            _state.update { it.copy(errorMessage = "Token is required.") }
            return
        }
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithPersonalAccessToken(token)
            when (result) {
                is GithubAuthResult.Success -> _state.value = AuthUiState(
                    authState = result.state,
                    tokenInput = "",
                    formatLooksValid = false,
                )
                is GithubAuthResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        tokenInput = "",
                        formatLooksValid = false,
                        errorMessage = result.reason,
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthUiState()
        }
    }

    private fun refreshAuthState() {
        viewModelScope.launch {
            _state.update { it.copy(authState = authRepository.authState()) }
        }
    }

    companion object {
        fun factory(authRepository: GithubAuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    AuthViewModel(authRepository) as T
            }
    }
}

data class AuthUiState(
    val authState: GithubAuthState = GithubAuthState.Unauthenticated,
    val tokenInput: String = "",
    val formatLooksValid: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val isAuthenticated: Boolean get() = authState is GithubAuthState.Authenticated
    val canSubmit: Boolean get() = !isSubmitting && tokenInput.isNotBlank()
}
