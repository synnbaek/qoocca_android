package com.qoocca.parentapp.presentation.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.data.repository.ReceiptRepository
import com.qoocca.parentapp.presentation.common.UiMessageFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val receiptRepository = ReceiptRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

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
            _events.tryEmit(MainEvent.NavigateLogin)
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = if (isRefresh) _uiState.value.isLoading else true,
            isRefreshing = isRefresh,
            error = null
        )

        receiptRepository.fetchReceiptRequests(token) { result ->
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        receipts = result.data,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }

                is ApiResult.NetworkError -> {
                    Log.e(TAG, "결제 목록 조회 실패: ${result.exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = UiMessageFactory.receiptListFailure(result)
                    )
                }

                is ApiResult.UnknownError -> {
                    Log.e(TAG, "응답 파싱 에러: ${result.exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = UiMessageFactory.receiptListFailure(result)
                    )
                }

                is ApiResult.HttpError -> {
                    Log.e(TAG, "결제 목록 조회 에러: ${result.code} - ${result.body}")
                    if (result.code == 403) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiMessageFactory.receiptListFailure(result)
                        )
                        _events.tryEmit(MainEvent.NavigateLogin)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiMessageFactory.receiptListFailure(result)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MAIN_ACTIVITY_DEBUG"
    }
}
