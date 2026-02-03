package com.qoocca.parentapp.presentation.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.data.repository.PaymentRepository
import com.qoocca.parentapp.data.repository.ReceiptRepository
import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.result.AppResult
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
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val receiptRepository = ReceiptRepository()
    private val paymentRepository = PaymentRepository()

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

        val failedCount = AtomicInteger(0)
        val authFailed = AtomicInteger(0)
        val remaining = AtomicInteger(receiptIds.size)

        receiptIds.forEach { id ->
            paymentRepository.payReceipt(token, id) { result ->
                when (result) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        Log.e(TAG, "결제 실패: ${result.error}")
                        AppEventLogger.logEvent(getApplication(), "payment_item_failure", mapOf("error" to result.error.toString()))
                        failedCount.incrementAndGet()
                        if (result.error == AppError.Unauthorized || result.error == AppError.Forbidden) {
                            authFailed.incrementAndGet()
                        }
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    val failed = failedCount.get()
                    onPaymentsCompleted(failed)
                    if (authFailed.get() > 0) {
                        AuthSessionManager.onAuthFailure(getApplication())
                    }
                }
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

        val receipts = Collections.synchronizedList(mutableListOf<ParentReceiptResponse>())
        val orderMap = receiptIds.withIndex().associate { it.value to it.index }
        val remaining = AtomicInteger(receiptIds.size)
        val authFailed = AtomicInteger(0)

        receiptIds.forEach { id ->
            receiptRepository.fetchReceiptDetail(token, id) { result ->
                when (result) {
                    is AppResult.Success -> receipts.add(result.data)
                    is AppResult.Failure -> {
                        Log.e(TAG, "결제 상세 조회 실패: ${result.error}")
                        AppEventLogger.logEvent(getApplication(), "payment_detail_failure", mapOf("error" to result.error.toString()))
                        if (result.error == AppError.Unauthorized || result.error == AppError.Forbidden) {
                            authFailed.incrementAndGet()
                        }
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    val sortedReceipts = receipts.sortedBy { orderMap[it.receiptId] ?: Int.MAX_VALUE }
                    _uiState.value = _uiState.value.copy(
                        receipts = sortedReceipts,
                        isLoading = false,
                        errorMessage = if (sortedReceipts.isEmpty()) UiMessageFactory.RECEIPT_FETCH_FAILED else null
                    )
                    AppEventLogger.logEvent(getApplication(), "payment_receipt_loaded", mapOf("count" to sortedReceipts.size))
                    if (authFailed.get() > 0) {
                        AuthSessionManager.onAuthFailure(getApplication())
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PAYMENT_ACTIVITY_DEBUG"
    }
}
