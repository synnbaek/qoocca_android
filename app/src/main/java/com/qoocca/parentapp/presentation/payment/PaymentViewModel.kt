package com.qoocca.parentapp.presentation.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.data.repository.PaymentRepository
import com.qoocca.parentapp.data.repository.ReceiptRepository
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
            return
        }

        fetchAllReceiptDetails(idsToFetch)
    }

    fun confirmPayments() {
        val token = authManager.getToken()
        if (token.isNullOrBlank()) {
            _events.tryEmit(PaymentEvent.NavigateLogin)
            return
        }

        val receiptIds = _uiState.value.receipts.map { it.receiptId }
        if (receiptIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = UiMessageFactory.NOTHING_TO_PAY)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        val failedCount = AtomicInteger(0)
        val remaining = AtomicInteger(receiptIds.size)

        receiptIds.forEach { id ->
            paymentRepository.payReceipt(token, id) { result ->
                when (result) {
                    is ApiResult.Success -> Unit
                    is ApiResult.HttpError -> {
                        Log.e(TAG, "결제 실패: ${result.code} - ${result.body}")
                        failedCount.incrementAndGet()
                    }
                    is ApiResult.NetworkError -> {
                        Log.e(TAG, "결제 실패: ${result.exception.message}")
                        failedCount.incrementAndGet()
                    }
                    is ApiResult.UnknownError -> {
                        Log.e(TAG, "결제 처리 실패: ${result.exception.message}")
                        failedCount.incrementAndGet()
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    val failed = failedCount.get()
                    onPaymentsCompleted(failed)
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
            _events.tryEmit(PaymentEvent.NavigateLogin)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val receipts = Collections.synchronizedList(mutableListOf<ParentReceiptResponse>())
        val orderMap = receiptIds.withIndex().associate { it.value to it.index }
        val remaining = AtomicInteger(receiptIds.size)

        receiptIds.forEach { id ->
            receiptRepository.fetchReceiptDetail(token, id) { result ->
                when (result) {
                    is ApiResult.Success -> receipts.add(result.data)
                    is ApiResult.HttpError -> Log.e(TAG, "결제 상세 조회 실패: ${result.code} - ${result.body}")
                    is ApiResult.NetworkError -> Log.e(TAG, "결제 상세 조회 네트워크 실패: ${result.exception.message}")
                    is ApiResult.UnknownError -> Log.e(TAG, "결제 상세 조회 파싱 실패: ${result.exception.message}")
                }

                if (remaining.decrementAndGet() == 0) {
                    val sortedReceipts = receipts.sortedBy { orderMap[it.receiptId] ?: Int.MAX_VALUE }
                    _uiState.value = _uiState.value.copy(
                        receipts = sortedReceipts,
                        isLoading = false,
                        errorMessage = if (sortedReceipts.isEmpty()) UiMessageFactory.RECEIPT_FETCH_FAILED else null
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "PAYMENT_ACTIVITY_DEBUG"
    }
}
