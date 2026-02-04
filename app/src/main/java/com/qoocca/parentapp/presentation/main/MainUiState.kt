package com.qoocca.parentapp.presentation.main

import com.qoocca.parentapp.ParentReceiptResponse

data class MainUiState(
    val receipts: List<ParentReceiptResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
