package com.qoocca.parentapp.presentation.payment

import com.qoocca.parentapp.ParentReceiptResponse

data class PaymentUiState(
    val receipts: List<ParentReceiptResponse> = emptyList(),
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface PaymentEvent {
    data object PaymentSuccess : PaymentEvent
    data object NavigateLogin : PaymentEvent
}
