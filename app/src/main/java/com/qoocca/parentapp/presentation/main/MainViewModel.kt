package com.qoocca.parentapp.presentation.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.data.repository.ReceiptRepository
import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.result.AppResult
import com.qoocca.parentapp.presentation.common.AuthSessionManager
import com.qoocca.parentapp.presentation.common.UiMessageFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val receiptRepository = ReceiptRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun isLoggedIn(): Boolean = authManager.isLoggedIn()

    fun loadInitial() {
        fetchReceipts(isRefresh = false)
    }

    fun refresh() {
        fetchReceipts(isRefresh = true)
    }

    private fun fetchReceipts(isRefresh: Boolean) {
        val token = authManager.getToken()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = UiMessageFactory.TOKEN_REQUIRED
            )
            AuthSessionManager.onAuthFailure(getApplication())
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = if (isRefresh) _uiState.value.isLoading else true,
            isRefreshing = isRefresh,
            error = null
        )

        receiptRepository.fetchReceiptRequests(token) { result ->
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        receipts = result.data,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }

                is AppResult.Failure -> {
                    Log.e(TAG, "결제 목록 조회 실패: ${result.error}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = UiMessageFactory.receiptListFailure(result.error)
                    )
                    if (result.error == AppError.Unauthorized || result.error == AppError.Forbidden) {
                        AuthSessionManager.onAuthFailure(getApplication())
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MAIN_ACTIVITY_DEBUG"
    }
}
