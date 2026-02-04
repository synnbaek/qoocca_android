package com.qoocca.parentapp.presentation.login

data class LoginUiState(
    val phone: String = "",
    val isLoading: Boolean = false
)

sealed interface LoginEvent {
    data class ShowMessage(val message: String) : LoginEvent
    data object NavigateMain : LoginEvent
}
