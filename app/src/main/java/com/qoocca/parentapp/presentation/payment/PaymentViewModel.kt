package com.qoocca.parentapp.presentation.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.domain.usecase.GetReceiptDetailsUseCase
import com.qoocca.parentapp.domain.usecase.PayReceiptsUseCase
import com.qoocca.parentapp.presentation.common.AppEventLogger
import com.qoocca.parentapp.presentation.common.AuthSessionManager
import com.qoocca.parentapp.presentation.common.UiMessageFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val getReceiptDetailsUseCase: GetReceiptDetailsUseCase,
    private val payReceiptsUseCase: PayReceiptsUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    private var messageJob: Job? = null

    fun initialize(receiptId: Long, receiptIds: LongArray?) {
        val idsToFetch = receiptIds?.toList() ?: if (receiptId != -1L) listOf(receiptId) else emptyList()

        if (idsToFetch.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = UiMessageFactory.RECEIPT_NOT_FOUND
            )
            AppEventLogger.logEvent(getApplication(), "payment_init_invalid")
            return
        }

        fetchAllReceiptDetails(idsToFetch)
    }

    fun confirmPayments() {
        val token = authManager.getToken()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = UiMessageFactory.TOKEN_REQUIRED)
            AuthSessionManager.onAuthFailure(getApplication())
            AppEventLogger.logEvent(getApplication(), "payment_confirm_no_token")
            return
        }

        val receiptIds = _uiState.value.receipts.map { it.receiptId }
        if (receiptIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = UiMessageFactory.NOTHING_TO_PAY)
            AppEventLogger.logEvent(getApplication(), "payment_confirm_empty")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        AppEventLogger.logEvent(getApplication(), "payment_confirm_started", mapOf("count" to receiptIds.size))

        payReceiptsUseCase.execute(token, receiptIds) { outcome ->
            outcome.errors.forEach { error ->
                Log.e(TAG, "결제 실패: $error")
                AppEventLogger.logEvent(getApplication(), "payment_item_failure", mapOf("error" to error.toString()))
            }

            onPaymentsCompleted(outcome.failedCount)
            if (outcome.hasAuthFailure) {
                AuthSessionManager.onAuthFailure(getApplication())
            }
        }
    }

    private fun onPaymentsCompleted(failedCount: Int) {
        if (failedCount == 0) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                infoMessage = UiMessageFactory.PAYMENT_SUCCESS
            )
            AppEventLogger.logEvent(getApplication(), "payment_success", mapOf("count" to _uiState.value.receipts.size))
            messageJob?.cancel()
            messageJob = viewModelScope.launch {
                delay(2000)
                _events.tryEmit(PaymentEvent.PaymentSuccess)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                infoMessage = null,
                errorMessage = UiMessageFactory.paymentFailedCount(failedCount)
            )
            AppEventLogger.logEvent(getApplication(), "payment_partial_failure", mapOf("failed_count" to failedCount))
            messageJob?.cancel()
            messageJob = viewModelScope.launch {
                delay(3000)
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun fetchAllReceiptDetails(receiptIds: List<Long>) {
        val token = authManager.getToken()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = UiMessageFactory.TOKEN_REQUIRED)
            AuthSessionManager.onAuthFailure(getApplication())
            AppEventLogger.logEvent(getApplication(), "payment_detail_no_token")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        getReceiptDetailsUseCase.execute(token, receiptIds) { outcome ->
            outcome.errors.forEach { error ->
                Log.e(TAG, "결제 상세 조회 실패: $error")
                AppEventLogger.logEvent(getApplication(), "payment_detail_failure", mapOf("error" to error.toString()))
            }

            _uiState.value = _uiState.value.copy(
                receipts = outcome.receipts,
                isLoading = false,
                errorMessage = if (outcome.receipts.isEmpty()) UiMessageFactory.RECEIPT_FETCH_FAILED else null
            )
            AppEventLogger.logEvent(getApplication(), "payment_receipt_loaded", mapOf("count" to outcome.receipts.size))
            if (outcome.hasAuthFailure) {
                AuthSessionManager.onAuthFailure(getApplication())
            }
        }
    }

    companion object {
        private const val TAG = "PAYMENT_ACTIVITY_DEBUG"
    }
}
